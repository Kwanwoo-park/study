# Kafka 운영과 실패 복구

관리자 홈의 **Kafka 운영 · 실패 재처리**에서 `/admin/kafka`로 이동합니다. 관리자만 조회·재발행할 수 있으며 재발행 POST에는 CSRF 확인도 적용합니다.

## 전달과 실패 처리

- 기존 채팅 `topic`(key=roomId), 알림 `topic2`(key=memberId) 이름을 유지합니다.
- MySQL outbox → Kafka 발행 시 `kwanwoo-event-id=outbox-{id}` header를 붙여 재발행 사이에도 식별자를 유지합니다.
- Consumer는 일시 오류에 기본 1초 간격 3회 재시도 후 `topic.DLT` / `topic2.DLT`로 옮깁니다. 역직렬화 등 복구 불가능한 오류는 바로 DLT로 보낼 수 있습니다.
- DLT 발행의 broker ACK를 확인해야 원본 처리를 넘깁니다. DLT 발행이 실패하면 원본 메시지를 다시 처리합니다. 정상 객체와 역직렬화에 실패한 원본 byte[] 모두 보관됩니다.
- 채팅은 배치 트랜잭션입니다. 실패 레코드를 안전하게 특정할 수 없는 오류는 **전체 실패 배치**를 재시도·DLT 보관합니다. 일부 레코드의 offset만 먼저 커밋해 나머지를 잃는 방식은 사용하지 않습니다.
- DLT는 별도 소비자 그룹 `{group-id}.dead-letter-store`에서 읽어 `kafka_dead_letter`에 보관합니다. DB 장애 시 포기하지 않고 다시 시도합니다. 원본 topic/partition/offset에 유일 제약을 두어 중복 보관을 방지합니다.
- 이 테이블에는 재발행에 필요한 원본 본문과 key가 저장됩니다. 관리자 목록 API는 본문/key/event header를 반환하지 않고 메타데이터만 조회합니다. 진단용 `integration_event_log`에는 본문을 저장하지 않습니다.

## 재발행

1. 관리자 요청을 DB에 `REPLAY_PENDING`으로 저장하고 HTTP 202를 반환합니다. 중복 클릭·동시 요청은 행 잠금과 상태 검사로 막습니다.
2. 작업자가 기본 5초마다 요청을 확인해 원래 토픽·key·이벤트 식별자로 다시 발행합니다. 원본 Kafka offset을 임의로 되돌리지 않습니다.
3. broker ACK 이후에만 `REPLAYED`로 표시합니다. 발행 실패는 5초부터 최대 30분 간격으로 재시도하고, 10회 실패하면 `REPLAY_FAILED`로 남겨 다시 요청할 수 있게 합니다.
4. 재발행 후 Consumer가 다시 실패하면 새 source offset을 가진 별도 DLT 기록이 생깁니다. `REPLAYED`는 최종 소비나 사용자 수신 완료가 아닙니다.

재발행은 현재 로그의 끝에 추가되므로 원래 발생 순서가 보존되지 않습니다. 일반적인 재발행은 at-least-once입니다. broker ACK 이후 DB 커밋 직전에 프로세스가 죽으면 동일 이벤트가 다시 발행될 수 있습니다.

발행 완료된 DB 기록은 기본 30일 후 정리합니다. 미처리·재발행 실패 기록은 자동 삭제하지 않습니다. DLT 토픽에는 기본 14일 보관을 적용하므로 DB 아카이브 장애가 이보다 길어지기 전에 복구해야 합니다. Outbox의 `deadLettered=true`는 별도의 **발행 전 실패** 상태이며 이 화면의 소비 실패 재발행 대상과 다릅니다.

## 중복 전달과 오래된 상태

- 채팅은 DB ID 중복 저장을 막고, 같은 배치의 동일 ID를 한 번만 전달합니다. Redis 전달 완료 표시가 있으면 WebSocket 재전송을 생략합니다. 표시는 송신 후 기록해 DB 저장 뒤 송신 실패가 생겨도 재시도할 수 있습니다.
- WebSocket 송신과 Redis 표시 사이의 종료까지 원자적으로 묶을 수는 없으므로 클라이언트의 기존 메시지 ID 중복 제거도 유지합니다. 소켓·SSE 단계는 최종 사용자 수신을 보장하지 않습니다.
- 기존 채팅이 삭제·수정된 경우 오래된 생성 이벤트를 다시 방송하지 않습니다.
- 알림은 DB의 최신 알림 상태를 다시 읽습니다. Redis Lua로 이벤트 ID 확인과 Pub/Sub 발행을 한 연산에서 처리합니다. 기존 header 없는 이벤트는 알림 내용·상태·갱신 시각의 해시를 사용합니다. 구독 서버가 없을 때는 중복 방지 표시를 남기지 않습니다.
- 중복 방지 key는 기본 30일 TTL이며 Redis 데이터가 제거되거나 TTL이 지나면 중복 전달될 수 있습니다. 여러 애플리케이션 인스턴스로 채팅을 운영할 때는 기존 로컬 WebSocket broker의 인스턴스 간 fan-out도 별도로 구성해야 합니다.

## 지연과 토픽 설정

운영 페이지에서 main 그룹과 DLT 보관 그룹의 파티션별 committed/end offset, lag, outbox 최장 대기 시간과 실패 건수를 확인합니다. broker 조회는 서버에서 10초 캐시하고 화면은 15초마다 갱신합니다. 커밋 위치를 모르는 비어 있지 않은 파티션은 지연을 0으로 표시하지 않습니다. 현재 경고는 운영 화면에 표시하며 별도 푸시 알림은 보내지 않습니다.

시작 시 누락된 4개 토픽만 생성합니다. **기존 토픽의 파티션 수·복제 수·보관 정책은 자동 수정하지 않으며**, 설정값과 다르면 경고합니다. 기존 토픽은 운영자가 Kafka 관리 도구로 계획적으로 조정해야 합니다. 파티션 수 변경은 같은 key가 다른 파티션으로 이동할 수 있어 채팅 순서에 영향을 줍니다.

설정 기본값은 단일 broker에 맞췄습니다. `acks=all`만으로 복제본이 추가되지는 않습니다. 다중 broker로 전환할 때 실제 broker 수에 맞춰 replication-factor/min-in-sync-replicas를 조정하세요.

```properties
kafka.operations.provision-topics=true
kafka.operations.chat-partitions=1
kafka.operations.notification-partitions=1
kafka.operations.dead-letter-partitions=1
kafka.operations.replication-factor=1
kafka.operations.min-in-sync-replicas=1
kafka.operations.retention-days=7
kafka.operations.dead-letter-retention-days=14
kafka.operations.consumer-retries=3
kafka.operations.consumer-retry-delay-ms=1000
kafka.operations.chat-concurrency=1
kafka.operations.notification-concurrency=1
kafka.operations.lag-warning-threshold=100
kafka.operations.outbox-warning-seconds=60
kafka.operations.dedup-ttl-seconds=2592000
kafka.operations.replay-history-days=30
kafka.operations.replay-poll-ms=5000
kafka.operations.cleanup-cron=0 30 3 * * *
```

토픽을 인프라에서 생성하는 환경은 `provision-topics=false`로 설정하고 DLT 두 개도 사전 생성하세요. 소비자의 자동 토픽 생성은 꺼져 있습니다. DLT는 파티션을 독립적으로 선택하므로 원본과 파티션 수가 같을 필요는 없습니다. Kafka Streams/Connect는 추가하지 않습니다.

`ddl-auto=update`이면 새 테이블이 생성됩니다. 자동 DDL을 쓰지 않는 배포 환경은 [수동 MySQL SQL](sql/kafka-dead-letter.sql)을 먼저 적용하세요. Flyway는 사용하지 않습니다.
