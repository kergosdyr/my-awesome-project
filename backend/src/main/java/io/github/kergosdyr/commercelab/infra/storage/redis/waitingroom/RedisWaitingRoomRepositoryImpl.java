package io.github.kergosdyr.commercelab.infra.storage.redis.waitingroom;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import io.github.kergosdyr.commercelab.domain.waitingroom.WaitingRoomRepository;
import io.github.kergosdyr.commercelab.support.error.ApiException;
import io.github.kergosdyr.commercelab.support.error.ErrorType;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class RedisWaitingRoomRepositoryImpl implements WaitingRoomRepository {

    private static final String PREFIX = "lab:{waiting-room}:";
    private static final String QUEUE_KEY = PREFIX + "queue";
    private static final String TICKET_EXPIRY_KEY = PREFIX + "ticket-expiry";
    private static final String ACTIVE_KEY = PREFIX + "active";
    private static final String SEQUENCE_KEY = PREFIX + "sequence";
    private static final String METRICS_KEY = PREFIX + "metrics";
    private static final String KNOWN_KEYS_KEY = PREFIX + "known-keys";
    private static final String TICKET_KEY_PREFIX = PREFIX + "ticket:";
    private static final String ADMISSION_KEY_PREFIX = PREFIX + "admission:";

    private static final RedisScript<String> ENQUEUE_SCRIPT = script("""
            local expired = redis.call('ZRANGEBYSCORE', KEYS[2], '-inf', ARGV[3])
            for _, ticketId in ipairs(expired) do
                redis.call('ZREM', KEYS[1], ticketId)
            end
            if #expired > 0 then
                redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', ARGV[3])
                redis.call('HINCRBY', KEYS[4], 'expiredTickets', #expired)
            end

            local sequence = redis.call('INCR', KEYS[3])
            redis.call('ZADD', KEYS[1], sequence, ARGV[1])
            redis.call('ZADD', KEYS[2], ARGV[4], ARGV[1])
            redis.call('HSET', KEYS[5],
                'state', 'QUEUED',
                'reservedAdmissionToken', ARGV[2],
                'issuedAt', ARGV[3],
                'expiresAt', ARGV[4],
                'sequence', sequence)
            redis.call('PEXPIREAT', KEYS[5], ARGV[4])
            redis.call('SADD', KEYS[6], KEYS[5])
            redis.call('HINCRBY', KEYS[4], 'issued', 1)

            local depth = redis.call('ZCARD', KEYS[1])
            local maxDepth = tonumber(redis.call('HGET', KEYS[4], 'maxQueueDepth') or '0')
            if depth > maxDepth then
                redis.call('HSET', KEYS[4], 'maxQueueDepth', depth)
            end
            return sequence .. '|' .. depth
            """);

    private static final RedisScript<String> POLL_SCRIPT = script("""
            local expiredTickets = redis.call('ZRANGEBYSCORE', KEYS[2], '-inf', ARGV[2])
            for _, expiredTicketId in ipairs(expiredTickets) do
                redis.call('ZREM', KEYS[1], expiredTicketId)
            end
            if #expiredTickets > 0 then
                redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', ARGV[2])
                redis.call('HINCRBY', KEYS[4], 'expiredTickets', #expiredTickets)
            end

            local expiredAdmissions = redis.call('ZRANGEBYSCORE', KEYS[3], '-inf', ARGV[2])
            if #expiredAdmissions > 0 then
                redis.call('ZREMRANGEBYSCORE', KEYS[3], '-inf', ARGV[2])
                redis.call('HINCRBY', KEYS[4], 'expiredAdmissions', #expiredAdmissions)
            end

            if redis.call('EXISTS', KEYS[5]) == 1
                    and redis.call('HGET', KEYS[5], 'state') == 'QUEUED' then
                redis.call('HSET', KEYS[5], 'lastPolledAt', ARGV[2])
            end

            local active = redis.call('ZCARD', KEYS[3])
            local available = tonumber(ARGV[5]) - active
            while available > 0 do
                local head = redis.call('ZRANGE', KEYS[1], 0, 0)[1]
                if not head then
                    break
                end

                local headTicketKey = ARGV[6] .. head
                if redis.call('EXISTS', headTicketKey) == 0 then
                    redis.call('ZREM', KEYS[1], head)
                    redis.call('ZREM', KEYS[2], head)
                    redis.call('HINCRBY', KEYS[4], 'expiredTickets', 1)
                else
                    local lastPolledAt = tonumber(redis.call('HGET', headTicketKey, 'lastPolledAt'))
                    if not lastPolledAt or lastPolledAt < tonumber(ARGV[4]) then
                        break
                    end

                    local token = redis.call('HGET', headTicketKey, 'reservedAdmissionToken')
                    local admissionKey = ARGV[7] .. token
                    local sequence = tonumber(redis.call('HGET', headTicketKey, 'sequence'))
                    local lastSequence = tonumber(
                        redis.call('HGET', KEYS[4], 'lastAdmittedSequence') or '0'
                    )
                    if sequence < lastSequence then
                        redis.call('HINCRBY', KEYS[4], 'fifoViolations', 1)
                    end
                    redis.call('HSET', KEYS[4], 'lastAdmittedSequence', sequence)

                    local issuedAt = tonumber(redis.call('HGET', headTicketKey, 'issuedAt'))
                    local waitDuration = tonumber(ARGV[2]) - issuedAt
                    redis.call('HINCRBY', KEYS[4], 'admitted', 1)
                    redis.call('HINCRBY', KEYS[4], 'totalWaitDurationMillis', waitDuration)
                    local maxWait = tonumber(
                        redis.call('HGET', KEYS[4], 'maxWaitDurationMillis') or '0'
                    )
                    if waitDuration > maxWait then
                        redis.call('HSET', KEYS[4], 'maxWaitDurationMillis', waitDuration)
                    end

                    redis.call('ZREM', KEYS[1], head)
                    redis.call('ZREM', KEYS[2], head)
                    redis.call('ZADD', KEYS[3], ARGV[3], token)
                    redis.call('HSET', headTicketKey,
                        'state', 'ADMITTED',
                        'token', token,
                        'admittedAt', ARGV[2],
                        'admissionExpiresAt', ARGV[3])
                    redis.call('PEXPIREAT', headTicketKey, ARGV[3])
                    redis.call('HSET', admissionKey,
                        'state', 'READY',
                        'ticketId', head)
                    redis.call('PEXPIREAT', admissionKey, ARGV[3])
                    redis.call('SADD', KEYS[6], admissionKey)
                    available = available - 1
                    active = active + 1
                end
            end

            local maxActive = tonumber(redis.call('HGET', KEYS[4], 'maxActive') or '0')
            if active > maxActive then
                redis.call('HSET', KEYS[4], 'maxActive', active)
            end

            if redis.call('EXISTS', KEYS[5]) == 0 then
                redis.call('ZREM', KEYS[1], ARGV[1])
                redis.call('ZREM', KEYS[2], ARGV[1])
                return 'EXPIRED'
            end

            local state = redis.call('HGET', KEYS[5], 'state')
            if state == 'ADMITTED' then
                local storedToken = redis.call('HGET', KEYS[5], 'token')
                local activeScore = redis.call('ZSCORE', KEYS[3], storedToken)
                if activeScore then
                    local admittedAt = tonumber(redis.call('HGET', KEYS[5], 'admittedAt'))
                    local issuedAt = tonumber(redis.call('HGET', KEYS[5], 'issuedAt'))
                    return 'ADMITTED|' .. storedToken .. '|' .. math.floor(activeScore)
                        .. '|' .. (admittedAt - issuedAt)
                end
                redis.call('DEL', KEYS[5])
                return 'EXPIRED'
            end

            local rank = redis.call('ZRANK', KEYS[1], ARGV[1])
            if not rank then
                return 'EXPIRED'
            end
            return 'QUEUED|' .. (rank + 1)
            """);

    private static final RedisScript<String> CLAIM_SCRIPT = script("""
            local expiredAdmissions = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[3])
            if #expiredAdmissions > 0 then
                redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[3])
                redis.call('HINCRBY', KEYS[2], 'expiredAdmissions', #expiredAdmissions)
            end

            if redis.call('EXISTS', KEYS[3]) == 0 then
                return 'EXPIRED'
            end
            local state = redis.call('HGET', KEYS[3], 'state')
            local storedToken = redis.call('HGET', KEYS[3], 'token')
            if state ~= 'ADMITTED' or storedToken ~= ARGV[2] then
                redis.call('HINCRBY', KEYS[2], 'bypassRejected', 1)
                return 'INVALID'
            end
            if redis.call('EXISTS', KEYS[4]) == 0 or not redis.call('ZSCORE', KEYS[1], ARGV[2]) then
                redis.call('ZREM', KEYS[1], ARGV[2])
                redis.call('DEL', KEYS[3])
                return 'EXPIRED'
            end
            if redis.call('HGET', KEYS[4], 'state') ~= 'READY'
                    or redis.call('HGET', KEYS[4], 'ticketId') ~= ARGV[1] then
                redis.call('HINCRBY', KEYS[2], 'bypassRejected', 1)
                return 'INVALID'
            end

            redis.call('HSET', KEYS[4], 'state', 'PROCESSING')
            redis.call('PEXPIREAT', KEYS[4], ARGV[4])
            redis.call('PEXPIREAT', KEYS[3], ARGV[4])
            redis.call('ZADD', KEYS[1], ARGV[4], ARGV[2])
            return 'CLAIMED'
            """);

    private static final RedisScript<String> RELEASE_SCRIPT = script("""
            redis.call('ZREM', KEYS[1], ARGV[2])
            redis.call('DEL', KEYS[3])
            redis.call('DEL', KEYS[4])
            redis.call('SREM', KEYS[5], KEYS[3], KEYS[4])
            if ARGV[3] == '1' then
                redis.call('HINCRBY', KEYS[2], 'completed', 1)
            else
                redis.call('HINCRBY', KEYS[2], 'downstreamRejected', 1)
            end
            return 'RELEASED'
            """);

    private static final RedisScript<String> METRICS_SCRIPT = script("""
            local expiredTickets = redis.call('ZRANGEBYSCORE', KEYS[2], '-inf', ARGV[1])
            for _, ticketId in ipairs(expiredTickets) do
                redis.call('ZREM', KEYS[1], ticketId)
            end
            if #expiredTickets > 0 then
                redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', ARGV[1])
                redis.call('HINCRBY', KEYS[4], 'expiredTickets', #expiredTickets)
            end
            local expiredAdmissions = redis.call('ZRANGEBYSCORE', KEYS[3], '-inf', ARGV[1])
            if #expiredAdmissions > 0 then
                redis.call('ZREMRANGEBYSCORE', KEYS[3], '-inf', ARGV[1])
                redis.call('HINCRBY', KEYS[4], 'expiredAdmissions', #expiredAdmissions)
            end

            local fields = {
                'issued', 'admitted', 'completed', 'downstreamRejected', 'bypassRejected',
                'expiredTickets', 'expiredAdmissions', 'maxQueueDepth', 'maxActive',
                'fifoViolations', 'lastAdmittedSequence', 'totalWaitDurationMillis',
                'maxWaitDurationMillis'
            }
            local values = {}
            for _, field in ipairs(fields) do
                table.insert(values, redis.call('HGET', KEYS[4], field) or '0')
            end
            return table.concat(values, '|') .. '|' .. redis.call('ZCARD', KEYS[1])
                .. '|' .. redis.call('ZCARD', KEYS[3])
            """);

    private final StringRedisTemplate redisTemplate;

    public RedisWaitingRoomRepositoryImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public EnqueuedTicket enqueue(
            String ticketId,
            String reservedAdmissionToken,
            Instant issuedAt,
            Instant expiresAt
    ) {
        var result = execute(
                ENQUEUE_SCRIPT,
                List.of(
                        QUEUE_KEY,
                        TICKET_EXPIRY_KEY,
                        SEQUENCE_KEY,
                        METRICS_KEY,
                        ticketKey(ticketId),
                        KNOWN_KEYS_KEY
                ),
                ticketId,
                reservedAdmissionToken,
                epochMillis(issuedAt),
                epochMillis(expiresAt)
        );
        var values = split(result, 2);
        return new EnqueuedTicket(longValue(values[0]), longValue(values[1]));
    }

    @Override
    public PollDecision poll(
            String ticketId,
            Instant now,
            Instant admissionExpiresAt,
            Duration promotionReadinessTtl,
            int maxConcurrency
    ) {
        var result = execute(
                POLL_SCRIPT,
                List.of(
                        QUEUE_KEY,
                        TICKET_EXPIRY_KEY,
                        ACTIVE_KEY,
                        METRICS_KEY,
                        ticketKey(ticketId),
                        KNOWN_KEYS_KEY
                ),
                ticketId,
                epochMillis(now),
                epochMillis(admissionExpiresAt),
                readinessCutoffMillis(now, promotionReadinessTtl),
                maxConcurrency,
                TICKET_KEY_PREFIX,
                ADMISSION_KEY_PREFIX
        );
        var values = result.split("\\|", -1);
        return switch (values[0]) {
            case "QUEUED" -> PollDecision.queued(longValue(values[1]));
            case "ADMITTED" -> PollDecision.admitted(
                    values[1],
                    Instant.ofEpochMilli(longValue(values[2])),
                    longValue(values[3])
            );
            case "EXPIRED" -> PollDecision.expired();
            default -> throw new IllegalStateException("Unexpected waiting-room poll result: " + values[0]);
        };
    }

    @Override
    public ClaimStatus claim(
            String ticketId,
            String admissionToken,
            Instant now,
            Instant processingLeaseExpiresAt
    ) {
        var result = execute(
                CLAIM_SCRIPT,
                List.of(
                        ACTIVE_KEY,
                        METRICS_KEY,
                        ticketKey(ticketId),
                        admissionKey(admissionToken),
                        KNOWN_KEYS_KEY
                ),
                ticketId,
                admissionToken,
                epochMillis(now),
                epochMillis(processingLeaseExpiresAt)
        );
        return ClaimStatus.valueOf(result);
    }

    @Override
    public void release(String ticketId, String admissionToken, boolean completed) {
        execute(
                RELEASE_SCRIPT,
                List.of(
                        ACTIVE_KEY,
                        METRICS_KEY,
                        ticketKey(ticketId),
                        admissionKey(admissionToken),
                        KNOWN_KEYS_KEY
                ),
                ticketId,
                admissionToken,
                completed ? 1 : 0
        );
    }

    @Override
    public Metrics metrics(Instant now) {
        var result = execute(
                METRICS_SCRIPT,
                List.of(QUEUE_KEY, TICKET_EXPIRY_KEY, ACTIVE_KEY, METRICS_KEY),
                epochMillis(now)
        );
        var values = split(result, 15);
        return new Metrics(
                longValue(values[0]),
                longValue(values[1]),
                longValue(values[2]),
                longValue(values[3]),
                longValue(values[4]),
                longValue(values[5]),
                longValue(values[6]),
                longValue(values[13]),
                longValue(values[7]),
                longValue(values[14]),
                longValue(values[8]),
                longValue(values[9]),
                longValue(values[10]),
                longValue(values[11]),
                longValue(values[12])
        );
    }

    @Override
    public void reset() {
        try {
            var keys = new HashSet<String>();
            var knownKeys = redisTemplate.opsForSet().members(KNOWN_KEYS_KEY);
            if (knownKeys != null) {
                keys.addAll(knownKeys);
            }
            keys.addAll(List.of(
                    QUEUE_KEY,
                    TICKET_EXPIRY_KEY,
                    ACTIVE_KEY,
                    SEQUENCE_KEY,
                    METRICS_KEY,
                    KNOWN_KEYS_KEY
            ));
            redisTemplate.delete(keys);
        } catch (DataAccessException exception) {
            throw unavailable(exception);
        }
    }

    private String execute(RedisScript<String> script, List<String> keys, Object... arguments) {
        try {
            var serializedArguments = Arrays.stream(arguments)
                    .map(String::valueOf)
                    .toArray(String[]::new);
            var result = redisTemplate.execute(script, keys, (Object[]) serializedArguments);
            if (result == null) {
                throw new ApiException(ErrorType.WAITING_ROOM_UNAVAILABLE);
            }
            return result;
        } catch (DataAccessException exception) {
            throw unavailable(exception);
        }
    }

    private static RedisScript<String> script(String source) {
        return new DefaultRedisScript<>(source, String.class);
    }

    private static String ticketKey(String ticketId) {
        return TICKET_KEY_PREFIX + ticketId;
    }

    private static String admissionKey(String admissionToken) {
        return ADMISSION_KEY_PREFIX + admissionToken;
    }

    private static long epochMillis(Instant instant) {
        return instant.toEpochMilli();
    }

    private static long readinessCutoffMillis(Instant now, Duration readinessTtl) {
        try {
            return Math.subtractExact(epochMillis(now), readinessTtl.toMillis());
        } catch (ArithmeticException exception) {
            return Long.MIN_VALUE;
        }
    }

    private static String[] split(String value, int expectedSize) {
        var values = value.split("\\|", -1);
        if (values.length != expectedSize) {
            throw new IllegalStateException("Unexpected waiting-room Redis result size");
        }
        return values;
    }

    private static long longValue(String value) {
        return Long.parseLong(value);
    }

    private static ApiException unavailable(DataAccessException exception) {
        var unavailable = new ApiException(
                ErrorType.WAITING_ROOM_UNAVAILABLE,
                ErrorType.WAITING_ROOM_UNAVAILABLE.message()
        );
        unavailable.initCause(exception);
        return unavailable;
    }
}
