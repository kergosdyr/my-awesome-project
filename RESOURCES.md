# Redis 학습 자료

## Knowledge

- [Redis data types — Redis 공식 문서](https://redis.io/docs/latest/develop/data-types/)
  String, Hash, List, Set, Sorted Set, Stream을 포함한 Redis 논리 자료구조의 기준 문서다.
- [Compare data types — Redis 공식 문서](https://redis.io/docs/latest/develop/data-types/compare-data-types/)
  요구사항에 따라 자료구조를 선택할 때 사용한다.
- [Redis client handling — Redis 공식 문서](https://redis.io/docs/latest/develop/reference/clients/)
  논블로킹 소켓과 I/O 멀티플렉싱으로 여러 클라이언트를 다루는 방식을 확인할 때 사용한다.
- [Diagnosing latency issues — Redis 공식 문서](https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/latency/)
  Redis의 mostly-single-threaded 명령 실행과 느린 명령의 영향을 이해할 때 사용한다.
- [Scripting with Lua — Redis 공식 문서](https://redis.io/docs/latest/develop/programmability/eval-intro/)
  여러 Redis 명령을 하나의 원자적 연산으로 묶는 방법과 주의점을 확인할 때 사용한다.
- [OBJECT ENCODING — Redis 공식 문서](https://redis.io/docs/latest/commands/object-encoding/)
  논리 자료형이 실제로 어떤 내부 인코딩을 사용하는지 실행 중 확인할 때 사용한다.
- [Saga orchestration pattern — AWS Prescriptive Guidance](https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/saga-orchestration.html)
  2PC의 prepare/commit과 오케스트레이션 Saga의 로컬 트랜잭션·보상 차이를 비교할 때 사용한다.
- [Saga distributed transactions pattern — Azure Architecture Center](https://learn.microsoft.com/en-us/azure/architecture/patterns/saga)
  보상 가능 단계, 되돌릴 수 없는 pivot, 이후의 재시도 가능 단계를 구분할 때 사용한다.
- [PREPARE TRANSACTION — PostgreSQL 공식 문서](https://www.postgresql.org/docs/current/sql-prepare-transaction.html)
  2PC participant가 prepared 상태를 디스크에 보존하고 나중에 commit 또는 rollback되는 방식을 확인할 때 사용한다.
- [Explicit locking — PostgreSQL 공식 문서](https://www.postgresql.org/docs/current/explicit-locking.html)
  행 잠금의 대기, 교착 상태, 긴 트랜잭션의 비용을 확인할 때 사용한다.
- [Locking reads — MySQL 8.4 공식 문서](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html)
  `FOR UPDATE`, `NOWAIT`, `SKIP LOCKED`가 대기와 경합을 어떻게 다르게 처리하는지 확인할 때 사용한다.
- [Version — Jakarta Persistence API](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/version)
  `@Version` 기반 낙관적 락이 오래된 엔티티 갱신을 어떻게 탐지하는지 확인할 때 사용한다.
- [Shopify inventory reservations — Shopify Engineering](https://shopify.engineering/scaling-inventory-reservations)
  예약을 업무 상태로 모델링하고 `SKIP LOCKED`와 여러 sellable-unit 행으로 hot row를 분산한 사례다.
- [Clustered and Secondary Indexes — MySQL 8.4 공식 문서](https://dev.mysql.com/doc/refman/8.4/en/innodb-index-types.html)
  InnoDB 클러스터드 Leaf의 행 데이터와 세컨더리 레코드에 복제되는 PK의 관계를 확인할 때 사용한다.
- [Comparison of B-Tree and Hash Indexes — MySQL 8.4 공식 문서](https://dev.mysql.com/doc/refman/8.4/en/index-btree-hash.html)
  동등·범위·정렬·prefix LIKE에서 B-Tree와 Hash 인덱스의 지원 범위를 비교할 때 사용한다.
- [InnoDB Startup Configuration — MySQL 8.4 공식 문서](https://dev.mysql.com/doc/refman/8.4/en/innodb-init-startup-configuration.html)
  InnoDB 기본 페이지 크기와 저장 장치 특성에 따른 페이지 크기 trade-off를 확인할 때 사용한다.
- [Physical Structure of an InnoDB Index — MySQL 8.4 공식 문서](https://dev.mysql.com/doc/refman/8.4/en/innodb-physical-structure.html)
  Leaf 레코드의 페이지 저장, 순차·무작위 삽입에 따른 페이지 채움률, 페이지 병합 조건을 확인할 때 사용한다.
- [Optimizing InnoDB Disk I/O — MySQL 8.4 공식 문서](https://dev.mysql.com/doc/refman/8.4/en/optimizing-innodb-diskio.html)
  회전식 저장 장치와 비회전식 저장 장치에서 순차·랜덤 I/O 특성이 어떻게 다른지 확인할 때 사용한다.
- [Configuring InnoDB Buffer Pool Prefetching — MySQL 8.4 공식 문서](https://dev.mysql.com/doc/refman/8.4/en/innodb-performance-read_ahead.html)
  순차적인 페이지 접근을 감지해 다음 extent를 미리 읽는 linear read-ahead를 확인할 때 사용한다.
- [Indexes and Index-Organized Tables — Oracle AI Database 공식 문서](https://docs.oracle.com/en/database/oracle/oracle-database/26/cncpt/indexes-and-index-organized-tables.html)
  Oracle B-Tree의 branch·leaf block, heap table의 physical ROWID, IOT의 logical ROWID를 비교할 때 사용한다.
- [DB_BLOCK_SIZE — Oracle AI Database 공식 문서](https://docs.oracle.com/en/database/oracle/oracle-database/26/refrn/DB_BLOCK_SIZE.html)
  Oracle 표준 database block의 기본값과 설정 범위를 확인할 때 사용한다.
- [TreeMap — Java SE 25 공식 문서](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/TreeMap.html)
  Red-Black Tree 기반 정렬 Map의 연산과 탐색 복잡도를 확인할 때 사용한다.
- [TreeMap source — OpenJDK](https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/util/TreeMap.java)
  Red-Black Tree의 실제 노드 연결, 회전과 균형 유지 구현을 확인할 때 사용한다.

## Wisdom (Communities)

- [Redis GitHub Discussions](https://github.com/redis/redis/discussions)
  구현과 운영상의 선택을 Redis 사용자·기여자에게 검토받을 때 사용한다.
