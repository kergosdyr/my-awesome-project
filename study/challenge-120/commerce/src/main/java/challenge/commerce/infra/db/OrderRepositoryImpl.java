package challenge.commerce.infra.db;

import static challenge.commerce.infra.db.QOrderEntity.orderEntity;
import static challenge.commerce.infra.db.QPaymentEntity.paymentEntity;

import challenge.commerce.domain.order.*;
import challenge.commerce.domain.query.PageQuery;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepositoryImpl implements OrderRepository {
    private final OrderJpaRepository orderJpaRepository;
    private final JPAQueryFactory jpaQueryFactory;

    public OrderRepositoryImpl(OrderJpaRepository orderJpaRepository, JPAQueryFactory jpaQueryFactory) {
        this.orderJpaRepository = orderJpaRepository;
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public OrderEntity create(OrderEntity order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public Optional<OrderEntity> findById(long id) {
        return Optional.ofNullable(jpaQueryFactory
                .selectFrom(orderEntity)
                .leftJoin(orderEntity.items)
                .fetchJoin()
                .where(orderEntity.id.eq(id))
                .distinct()
                .fetchOne());
    }

    @Override
    public Optional<OrderDetailsResult> findDetailsById(long id) {
        return detailsQuery().where(orderEntity.id.eq(id)).fetch().stream()
                .map(OrderRepositoryImpl::details)
                .findFirst();
    }

    @Override
    public OrderPageResult findPage(PageQuery query) {
        var pageable = PageRequest.of(query.page(), query.size());
        // ID만 DB에서 제한하고 한 개 더 읽어 다음 페이지 유무를 확인한다.
        var fetchedIds = jpaQueryFactory
                .select(orderEntity.id)
                .from(orderEntity)
                .orderBy(orderEntity.createdAt.desc(), orderEntity.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1L)
                .fetch();
        boolean hasNext = fetchedIds.size() > query.size();
        var ids = fetchedIds.subList(0, Math.min(fetchedIds.size(), query.size()));
        if (ids.isEmpty()) return new OrderPageResult(List.of(), query.page(), query.size(), false);
        // 컬렉션 fetch join에는 페이지 제한을 적용하지 않고 선택된 ID만 조회한다.
        var entries = detailsQuery()
                .where(orderEntity.id.in(ids))
                .orderBy(orderEntity.createdAt.desc(), orderEntity.id.desc())
                .fetch()
                .stream()
                .map(OrderRepositoryImpl::details)
                .distinct()
                .toList();
        return new OrderPageResult(entries, query.page(), query.size(), hasNext);
    }

    @Override
    public OrderWindowResult findCursor(int size, OrderCursor cursor) {

        var fetched = detailsQuery()
                .where(cursorCondition(cursor))
                .orderBy(orderEntity.createdAt.desc(), orderEntity.id.desc())
                .limit(size + 1)
                .fetch()
                .stream()
                .map(OrderRepositoryImpl::details)
                .distinct()
                .toList();

        if (fetched.isEmpty()) {
            return new OrderWindowResult(List.of(), null);
        }

        var entries = fetched.subList(0, Math.min(size, fetched.size()));
        var lastOrder = entries.getLast().order();

        return new OrderWindowResult(
                entries, fetched.size() > size ? new OrderCursor(lastOrder.createdAt(), lastOrder.id()) : null);
    }

    private static BooleanExpression cursorCondition(OrderCursor cursor) {
        return cursor == null
                ? null
                : orderEntity
                        .createdAt
                        .before(cursor.createdAt())
                        .or(orderEntity.createdAt.eq(cursor.createdAt()).and(orderEntity.id.lt(cursor.id())));
    }

    private JPAQuery<Tuple> detailsQuery() {
        return jpaQueryFactory
                .select(orderEntity, paymentEntity)
                .distinct()
                .from(orderEntity)
                .leftJoin(orderEntity.items)
                .fetchJoin()
                .leftJoin(paymentEntity)
                .on(paymentEntity.orderId.eq(orderEntity.id));
    }

    private static OrderDetailsResult details(Tuple row) {
        return new OrderDetailsResult(row.get(orderEntity), row.get(paymentEntity));
    }
}
