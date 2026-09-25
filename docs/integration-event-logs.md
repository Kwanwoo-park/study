# Kafka · Redis 이벤트 로그

관리자 홈의 **최근 서버 장애 → Kafka · Redis 이벤트 로그**에서 `/admin/event-logs`로 이동합니다.
API는 `GET /api/admin/event-logs`이며 페이지와 API 모두 ADMIN 권한이 필요합니다. API는 비회원 401/일반 회원 403, 페이지는 404로 응답하고 캐시를 금지합니다.

## 기록 범위

- Kafka: Outbox 저장 완료(트랜잭션 커밋 후), 실제 발행 성공, 재시도 예정/10회 최종 실패, 원본 알림 삭제로 인한 건너뜀.
- Kafka Consumer: 채팅 batch 저장·WebSocket 전달 처리와 알림 처리의 성공/실패. 역직렬화 등 리스너 진입 전 오류 및 기존 오류 처리기의 재시도/처리 포기도 포함합니다.
- Redis: `notification-events` Pub/Sub 발행 성공/실패, 발행 시 Redis 구독 연결 수, 수신 후 SSE 처리 요청 성공/실패.
- 메시지 본문, Kafka key/header, 이메일, 토큰, 전체 예외 메시지/stack trace는 저장하지 않습니다. 오류는 원인 예외의 클래스명만 남깁니다.
- 일반 Redis 캐시 GET/SET, 통화 상태 Lua 연산, Kafka/Redis 서버 프로세스 자체의 원시 로그를 수집하는 기능은 아닙니다.

성공은 **해당 단계**만 의미합니다. Kafka 발행 완료가 Consumer 처리 완료를 뜻하지 않고, Redis 수신 처리 완료가 클라이언트 수신/읽음을 보장하지 않습니다. Redis 구독자 수는 사람이 아니라 서버의 구독 연결 수입니다. 채팅은 batch 단위로 건수를 기록하며 메시지 ID/본문은 남기지 않습니다.

`referenceId`는 Kafka QUEUE/PUBLISH에서 Outbox ID, 알림 Consumer/Redis에서 알림 ID입니다. Consumer 재시도와 채팅 batch에서는 비어 있습니다. `DEAD_LETTER`는 기존 Outbox의 최종 실패 상태이지 별도 DLT 전송이 아닙니다. `DISCARDED`는 기존 Kafka 오류 처리기가 처리를 포기한 결과입니다. 기존 Kafka 재시도/복구 정책은 변경하지 않습니다.

## 조회 / 보관

`broker=KAFKA|REDIS`, `operation=QUEUE|PUBLISH|CONSUME|CONSUMER_RETRY`, `outcome`으로 필터링합니다. `hours`는 1~720이며 실제 조회는 설정된 보관 기간 이내로 제한됩니다. 50개씩 기록 ID 내림차순으로 반환하고 `nextBeforeId`를 다음 요청의 `beforeId`로 넘깁니다. UI는 이전/다음, 수동 새로고침, 선택적인 10초 갱신을 지원합니다. 과거 페이지나 숨겨진 탭에서는 자동 갱신하지 않습니다. 시간은 서버 기준입니다.

기본 최근 7일을 MySQL `integration_event_log`에 저장합니다. 현재 프로젝트의 `spring.jpa.hibernate.ddl-auto=update`에서는 테이블을 자동 생성합니다. 자동 DDL을 사용하지 않는 환경은 배포 전 [수동 SQL](sql/integration-event-log.sql)을 적용하세요. Flyway는 추가하지 않습니다. 과거 이벤트는 소급 복원되지 않습니다.

## 장애 격리

발행/수신 스레드는 최대 2,000건의 메모리 버퍼에 메타데이터만 넣습니다. 전용 스레드가 1초마다 최대 100건을 별도 트랜잭션으로 저장하고, 1분마다 보관 기간이 지난 로그를 작은 묶음으로 정리합니다. 저장 실패 시 해당 묶음을 재시도하고 버퍼가 가득 차면 **로그만** 누락시킵니다. DB 장애 때문에 이벤트 전송을 막지 않습니다.

이 로그는 진단용 best-effort 기록이며 감사용 전달 보장 저장소가 아닙니다. 강제 종료 시 아직 저장하지 못한 버퍼, 과부하 시 새 로그가 유실될 수 있고 DB 커밋 응답 유실 시 중복될 수 있습니다. 페이지에 현재 서버 인스턴스의 대기/누락/저장 실패 누적 수를 표시합니다. 여러 서버의 영속 로그는 같은 DB에서 조회되며 각 행에 부팅별 인스턴스 ID가 남습니다.

선택 설정(기본값):

```properties
admin.event-log.enabled=true
admin.event-log.capacity=2000
admin.event-log.retention-days=7
```

용량은 1~10,000, 보관일은 1~30으로 제한합니다. `spring.study.scheduling.enabled=false`인 테스트 환경에서는 자동 저장/정리도 중지됩니다.

Kafka 오류 관찰은 [Spring Kafka 3.0 RetryListener](https://docs.spring.io/spring-kafka/docs/3.0.0/reference/html/#annotation-error-handling)의 콜백을 사용합니다.
