# Kafka UI 참고 및 프로젝트 통합 범위

## 참고 원본

- 프로젝트: [Provectus Kafka UI](https://github.com/provectus/kafka-ui)
- 확인 기준: `83b5a60cc08501b570a0c4d0b4cdfceb1b88d6b7` (2026-10-27 확인)
- 원본 라이선스: [Apache License 2.0](https://github.com/provectus/kafka-ui/blob/83b5a60cc08501b570a0c4d0b4cdfceb1b88d6b7/LICENSE)
- [BrokerService](https://github.com/provectus/kafka-ui/blob/83b5a60cc08501b570a0c4d0b4cdfceb1b88d6b7/kafka-ui-api/src/main/java/com/provectus/kafka/ui/service/BrokerService.java): 클러스터 노드·컨트롤러와 파티션 배치 정보를 함께 보는 구성.
- [TopicsService](https://github.com/provectus/kafka-ui/blob/83b5a60cc08501b570a0c4d0b4cdfceb1b88d6b7/kafka-ui-api/src/main/java/com/provectus/kafka/ui/service/TopicsService.java): 토픽 메타데이터·설정·시작/최신 오프셋을 묶어 상세 정보를 제공하는 구성.
- [ConsumerGroupService](https://github.com/provectus/kafka-ui/blob/83b5a60cc08501b570a0c4d0b4cdfceb1b88d6b7/kafka-ui-api/src/main/java/com/provectus/kafka/ui/service/ConsumerGroupService.java): 그룹 설명·할당·커밋 위치·최신 위치를 조합하는 조회 흐름.

위 소스의 기능 분류와 조회 흐름을 참고하여 기존 Spring MVC·Thymeleaf·JavaScript 구조에 맞춰 독립적으로 구현했습니다. 원본 Java/React 소스, 이미지, 로고, 스타일시트는 복사하거나 번들에 포함하지 않았습니다. Kafka UI 서버를 별도로 실행하거나 iframe으로 연결하지 않으며, Reactor/React와 신규 npm 의존성도 추가하지 않습니다.

## 제공하는 화면

기존 `/admin/kafka`와 `/api/admin/kafka/overview`를 확장했습니다.

- 개요: 클러스터 ID, 컨트롤러, Kafka 소비 지연과 기존 Outbox·실패·재발행 건수.
- 브로커: ID, 호스트/포트, 랙, 컨트롤러 여부, 운영 토픽의 리더·복제본 배치 수.
- 토픽: 이름 검색과 상세 진입. 파티션별 리더·복제본·ISR·시작/최신 오프셋, 공개 가능한 보관/복제 설정.
- 소비자 그룹: 그룹 검색, 상태·코디네이터·할당 방식·지연. 상세에서 멤버별 클라이언트/호스트/할당과 파티션별 소비 위치 확인.
- 실패·재처리 화면에서 Outbox 발행 최종 실패와 Kafka 소비 실패를 조회하고 CSRF 검증을 거쳐 재발행을 요청할 수 있습니다.

이 조회는 기존 운영 토픽 `topic`, `topic2`, `topic.DLT`, `topic2.DLT`와 설정된 main 그룹 및 `{group-id}.dead-letter-store` 그룹에 한정합니다. 임의의 다른 토픽·그룹을 탐색하거나 전체 클러스터의 토픽을 노출하지 않습니다.

## 계층과 조회 정책

- `KafkaClusterRepository`: Kafka Admin API 호출, 8초 전체 대기 예산, 10초 메타데이터 캐시.
- `KafkaClusterService`는 `KafkaClusterRepository`에서 브로커 상태를 조회합니다.
- `KafkaOperationsFacade`는 브로커 상태와 DB 통계를 합쳐 설정 불일치·복제 지연·리더 부재·처리 지연 경고를 제공합니다. `KafkaOperationsController`는 관리자용 조회와 재발행 요청을 받습니다.

한 토픽이나 그룹 조회가 실패해도 확인 가능한 다른 항목은 표시합니다. 예외 원문·접속 설정은 응답에 포함하지 않습니다. 토픽 설정도 보관/복제 관련 허용 목록만 노출하고 sensitive 설정은 제외합니다.

오프셋은 그룹별로 분리합니다. 서로 다른 그룹의 같은 토픽 커밋 위치를 덮어쓰지 않습니다. 커밋 위치가 없고 로그가 비어 있지 않거나, 커밋 위치가 보관 범위를 벗어나면 지연을 0으로 표시하지 않습니다. 전체 집계를 확정할 수 없을 때는 ‘미확인’을 표시합니다. 시작~최신 오프셋 차이는 삭제·컴팩션 등의 영향으로 정확한 메시지 개수가 아닐 수 있습니다.

화면은 기본 15초마다 갱신하며 자동 갱신을 끌 수 있습니다. 숨겨진 탭에서는 갱신하지 않고, 수동 새로고침·선택한 상세·실패 기록의 이전/다음 페이지는 유지합니다. 모바일 표는 표 내부에서 가로 스크롤되며 다크 모드를 지원합니다.

## 이번에 포함하지 않은 기능

메시지 본문 탐색·발행, 토픽 생성/삭제/설정 변경, 소비 오프셋 초기화, ACL 변경, 다중 클러스터 설정, Schema Registry/Connect 관리는 추가하지 않았습니다. 조회 도구가 기존 메시지 처리나 운영 설정을 변경하지 않도록 범위를 구분했습니다. 브로커 CPU·메모리·처리량·디스크 사용량 등은 JMX/별도 메트릭 수집을 연결한 것이 아니므로 이 화면에서 제공하지 않습니다.

기존 DLT 재발행은 계속 제공하지만, Kafka 발행 완료와 최종 소비/사용자 수신 완료를 구분하는 안내를 유지합니다. 새 DB 컬럼이나 배포용 별도 서비스는 필요하지 않습니다.
