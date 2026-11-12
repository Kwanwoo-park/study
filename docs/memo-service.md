# 관리자 개인 메모장

관리자 메인 화면의 **메모장** 버튼으로 `/admin/memos`에 접근합니다. 작성·수정·삭제, 제목/본문 검색, 상단 고정, 네 가지 색상, 페이지 단위 목록을 지원합니다. PC는 목록/편집창을 함께 표시하고, 모바일은 메모를 열면 편집창으로 전환합니다. 기존 테마 설정을 사용하며 화면 상단에서도 테마를 바꿀 수 있습니다.

메모 전용 코드는 `spring.study.memo` 아래의 `config`, `controller`, `dto`, `entity`, `facade`, `repository`, `service`로 분리되어 있습니다. 화면은 `templates/memo`, 정적 파일은 `static/css/memo`와 `static/js/memo`, 전용 테스트는 `src/test/java/spring/study/memo`에 있습니다. 도메인 분리는 소스 경로만 변경하며 기존 URL, 관리자 권한 제한, `admin_memo` 테이블은 유지합니다.

## 저장과 접근 범위

- 관리자라도 본인이 작성한 메모만 볼 수 있습니다. 클라이언트가 전달한 작성자 ID는 사용하지 않습니다.
- 제목은 최대 120자, 본문은 최대 20,000자입니다. 제목 없이 본문만 저장하면 `제목 없는 메모`라는 제목이 붙습니다.
- 저장 버튼 또는 `Cmd/Ctrl + S`로 DB에 저장합니다. 자동 저장은 아니며, 저장하지 않은 변경사항이 있으면 이동/종료 시 확인합니다.
- 다른 탭에서 먼저 변경한 메모를 과거 버전으로 덮어쓰거나 삭제하면 `409 Conflict`로 거절합니다. 작성 중인 내용은 화면에 유지되므로 복사 후 **저장본 다시 열기**를 사용합니다.
- 삭제는 확인 후 영구 삭제합니다. 회원 탈퇴 시에도 해당 회원의 메모를 함께 삭제합니다.
- 메모 내용은 브라우저 저장소에 보관하지 않습니다. 화면/API는 관리자 권한으로 보호하고, 쓰기 요청에는 별도 CSRF 토큰을 사용합니다. 응답에는 `Cache-Control: no-store`를 설정합니다.

## API

| 메서드 | 경로 | 용도 |
|---|---|---|
| GET | `/api/admin/memos/csrf` | 쓰기 요청용 `headerName`과 `token` |
| GET | `/api/admin/memos?query=&pinnedOnly=false&page=0` | 본인 메모 목록, 검색, 고정 필터. 페이지당 18개 |
| GET | `/api/admin/memos/{id}` | 본인 메모 상세 |
| POST | `/api/admin/memos` | 메모 작성 |
| PUT | `/api/admin/memos/{id}` | 내용/색상/고정 상태 수정 |
| DELETE | `/api/admin/memos/{id}?version=0` | 현재 버전 메모 삭제 |

작성/수정 JSON:

```json
{"title":"운영 노트","content":"다음 배포 전 확인할 내용","color":"MINT","pinned":true,"version":0}
```

색상은 `LAVENDER`, `MINT`, `AMBER`, `ROSE`입니다. 작성 시 `version`은 생략할 수 있으며 수정/삭제 시에는 상세 응답의 최신 값을 사용합니다. 목록 응답에는 본문 전체 대신 최대 160자의 미리보기만 포함합니다.

## DB 반영

현재 프로젝트의 `spring.jpa.hibernate.ddl-auto=update` 설정에서는 새 `admin_memo` 테이블을 Hibernate가 생성합니다. Flyway나 기존 데이터를 변경하는 시작 작업은 추가하지 않았습니다. DB를 수동 관리하는 환경에서는 아래 MySQL SQL을 적용 대상 DB에서 실행한 뒤 새 코드를 시작합니다. 개발 중 운영 DB에 이 SQL을 실행하지 않았습니다.

```sql
CREATE TABLE IF NOT EXISTS admin_memo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    content LONGTEXT NOT NULL,
    color VARCHAR(20) NOT NULL,
    pinned BIT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    register_time DATETIME(6) NULL,
    update_time DATETIME(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_admin_memo_owner_pinned_updated (owner_id, pinned, update_time, id),
    CONSTRAINT fk_admin_memo_owner FOREIGN KEY (owner_id) REFERENCES member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```
