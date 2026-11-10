# Repository 쿼리 점검

2026-11-04 기준 모든 도메인의 `repository` 패키지에 있는 Java 파일 36개를 확인했다.
직접 작성한 JPQL 37개 중 16개를 기본 CRUD 또는 메서드 이름 기반 쿼리로 전환했다.
나머지 21개는 아래의 집계·접근 제어·벌크 처리 등의 이유로 유지한다.

## 전환한 조회

| Repository | 변경 내용 |
| --- | --- |
| AccountRepository | 적금 납입 대상 조회, 출금 계좌를 사용하는 적금 존재 여부, 만기 도래 계좌 조회를 연관 속성 경로 기반 메서드로 전환 |
| SavingAutoTransferRepository | 적금 계좌 번호가 기본 키이므로 `findById`로 납입 설정을 읽어 출금 계좌 번호 확인 |
| AccountTransactionRepository | ID 기반 파생 조회 + `@Lock` + `@EntityGraph`로 전환, 입금/출금 계좌가 한쪽만 있는 거래도 유지 |
| ReportRepository | ID와 신고자 존재 조건 기반 파생 조회 + `@Lock` + 신고자 `@EntityGraph`로 전환 |
| NotificationRepository | 읽음 처리용 ID 조회에서 JPQL만 제거하고 기존 잠금 유지 |
| ChatRoomMemberRepository | 방 조건·회원 이름/ID 순 정렬을 파생 조회로 전환하고 회원 `@EntityGraph` 유지 |
| KafkaDeadLetterRepository | 재처리 상태·재시도 시각·ID 순 정렬·배치 크기·잠금을 파생 조회로 처리 |
| KafkaOutboxEventRepository | 발행 대상 배치 조회를 파생 조회로 전환, 최소 생성 시각 집계는 생성 시각만 읽는 정렬된 첫 행 조회로 전환 |
| BookReviewRepository | 제목·책 이름·저자 검색은 `LikeIgnoreCase` 파생 조회, 빈 검색어는 `findAll(Pageable)` 사용 |
| IntegrationEventLogRepository | 보관 기한이 지난 로그의 ID를 시간/ID 순으로 제한 조회 |
| RefreshTokenRepository | 회원 ID만 조회하는 `findDistinctBy` 프로젝션 사용 |
| MemberRepository | 후보 회원 중 미접속 또는 기준 시각 이전 접속 회원의 ID를 파생 조회로 확인 |
| FavoriteRepository | 회원과 게시글 ID 목록 조건의 파생 조회로 좋아요한 게시글 ID만 반환 |

계좌 번호·회원 ID·게시글 ID·생성 시각처럼 일부 컬럼만 필요했던 조회는 최상위 `record`
프로젝션으로 구성해 엔티티 전체를 읽지 않는다. 서비스의 기존 반환형과 외부 API는 유지한다.
잠금 조회는 단순 `findById`로 바꿔 잠금을 없애지 않고, 파생 조회에도 `@Lock`을 적용한다.

Spring Data 파생 쿼리에서는 `AND`가 `OR`보다 먼저 묶이므로 다음 두 조회의 조건은 양쪽에 반복한다.

- Outbox: `(미폐기 AND 재시도 시각 없음) OR (미폐기 AND 재시도 시각 도달)`
- 휴면 회원: `(후보 ID AND 접속 기록 없음) OR (후보 ID AND 마지막 접속 <= 기준 시각)`

독후감 검색의 `%`, `_`는 기존 JPQL과 같은 LIKE 패턴으로 유지한다.
`Containing`으로 바꿔 사용자가 입력한 와일드카드의 의미가 달라지지 않도록 한다.

## 유지한 JPQL 21개

| Repository / 메서드 | 개수 | 유지 이유 |
| --- | ---: | --- |
| IntegrationEventLogRepository.search | 1 | 선택적 broker/operation/outcome/커서 조건을 한 번에 처리. 메서드 조합을 늘리거나 전체 조회 후 필터링하지 않고 DB에서 제한 조회 |
| SystemIncidentRepository.acknowledgeAll | 1 | 장애 전체 확인을 단일 벌크 UPDATE로 처리 |
| AppealRepository.detachSanctions | 1 | 다른 엔티티 조건의 서브쿼리를 사용한 벌크 UPDATE |
| ChatMessageRepository.findVisibleByRoom / countVisibleUnread / countVisibleUnreadAfter | 3 | 개인별 숨김 NOT EXISTS 및 메시지 상태·읽은 시각 조건 보존 |
| ChatMessageImgRepository.findVisibleRoomImages | 1 | 연관 매핑이 없는 메시지 ID JOIN, 개인별 숨김 제외, 메시지 상태·검열·커서 조건과 DTO 생성 조회 |
| CommentRepository.countByBoardIds | 1 | 여러 게시글의 댓글 수를 GROUP BY 한 번으로 집계 |
| FavoriteRepository.countByBoardIds | 1 | 여러 게시글의 좋아요 수를 GROUP BY 한 번으로 집계 |
| FollowRepository의 공개 범위 적용 목록/개수 조회 | 4 | 공개·본인·팔로우 관계에 따른 EXISTS 접근 제어를 DB 조회/페이징 전에 적용 |
| ForbiddenRepository.updateStatusInIdList | 1 | 금칙어 상태 일괄 변경을 벌크 UPDATE로 처리 |
| KafkaDeadLetterRepository.findRecent | 1 | 선택적 상태·커서 조건과 DTO 생성자 프로젝션으로 필요한 컬럼과 payload 존재 여부만 조회. payload/key 원문은 조회 결과에서 제외 |
| KafkaDeadLetterRepository.deleteReplayedBefore | 1 | 완료된 실패 이벤트를 단일 벌크 DELETE로 처리. 파생 delete로 바꿔 엔티티를 모두 로딩하지 않음 |
| MemberRepository.updateLastLoginTime | 1 | 회원 전체를 로딩하지 않는 접속 시각 단일 UPDATE |
| NotificationRepository.updateAll / updateReadStatusByMemberAndGroupAndUrl | 2 | 확인 시각 이후 도착한 알림을 제외하는 COALESCE 조건과 벌크 UPDATE 보존 |
| NotificationRepository.findRecentByMember / findRecentByMemberAndGroup | 2 | `COALESCE(updateTime, registerTime)` 기준 최신순 정렬. 두 컬럼을 나란히 정렬하는 것으로 대체할 수 없음 |

단순 UPDATE/DELETE도 파생 조회 후 `save`/`delete` 반복으로 바꾸면 쿼리 수·메모리 사용량,
엔티티 콜백과 영속성 컨텍스트 동작이 달라지므로 기존 벌크 처리를 유지한다.

## 이미 기본 조회를 사용하던 Repository

BoardRepository, BoardImgRepository, AdminFileRepository, MemberSanctionRepository,
CollectionRepository, ChatRoomRepository, ChatMessageHiddenRepository, TodoRepository,
DiaryRepository, DiaryImageRepository, ReplyRepository는 불필요한 JPQL이 없어 그대로 유지했다.

UserServiceRepository, AudioCallStateStore, RedisAudioCallStateStore, KafkaClusterRepository,
EmitterRepository는 Redis·Kafka Admin·메모리 저장소 등 JPA와 다른 저장소 접근이므로
JpaRepository로 변경하지 않았다.

## 검증 기준

H2 실제 조회로 프로젝션, 회원 ID 중복 제거, OR 조건 범위, 날짜 경계, 배치 크기,
잠금 및 EntityGraph의 연관 로딩을 확인한다. 기존 Repository 테스트와 변경한 서비스/호출부
테스트도 함께 실행한다. 운영 DB·Redis·S3 데이터는 변경하지 않는다.

실행 결과: 관련 테스트 93개 통과. 전체 테스트는 488개 중 23개 실패, 1개 건너뜀이다.
전체 실행의 실패에는 인증/알림/채팅 등의 테스트 빈·mock 설정 누락 및 경로/기대값 불일치가
남아 있다. 이 작업에서 해당 기능의 동작을 변경하지는 않았다.

참고: 프로젝트 버전에 맞는 [Spring Data JPA 3.0.4 공식 문서](https://docs.spring.io/spring-data/jpa/docs/3.0.4/reference/html/)
의 query creation, projections, locking 항목을 기준으로 확인했다.
