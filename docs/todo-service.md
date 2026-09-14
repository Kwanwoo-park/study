# 독립 To-do list

일기와 연결되지 않는 회원 개인 할 일 기능입니다. 기존 일기의 할 일은 이전하지 않기로 했으며, 새 목록은 빈 상태로 시작합니다.

## 화면과 API

- 화면: `/todo` (`/todo/list`도 동일). 회원 상세의 `To-do list` 메뉴와 일기 목록에서 접근합니다.
- `GET /api/todo?page=0&size=20`: 본인 목록. `completed=true/false`로 완료 상태를 필터링합니다.
- `GET /api/todo/{id}`: 본인의 할 일 한 건 조회.
- `POST /api/todo`: `{ "content": "할 일" }`로 미완료 항목 작성.
- `PATCH /api/todo/{id}`: `{ "content": "수정 내용" }`로 내용만 변경.
- `PATCH /api/todo/{id}/completion`: `{ "completed": true }`로 완료 여부만 변경. `false`로 되돌릴 수 있습니다.
- `DELETE /api/todo/{id}`: 본인 항목 삭제.

내용은 공백만 입력할 수 없고 최대 255자입니다. 로그인 회원을 서버에서 결정하며, 다른 회원의 항목은 관리자라도 이 API로 조회·변경할 수 없습니다. 일기 삭제에는 영향을 받지 않고, 회원 탈퇴 시에는 해당 회원의 할 일을 삭제합니다.

## 기존 환경 배포 순서

이 문서의 SQL은 자동 실행되지 않습니다. Flyway나 시작 시 데이터 삭제 작업을 추가하지 않았습니다.

1. 새 코드를 빌드해 준비한 뒤 이전 애플리케이션을 중지합니다.
2. 적용 대상 DB가 맞는지 확인하고, 필요하면 백업합니다. 아래 `DROP TABLE`은 **기존 일기의 모든 할 일**을 삭제하며, 백업 없이는 복구할 수 없습니다. 일기 본문·이미지와 새 `todo` 데이터는 삭제하지 않습니다.
3. 새 테이블을 생성한 후, 더 이상 사용하지 않는 `diary_todo` 테이블을 삭제합니다. 기존 테이블을 남기면 `diary_id` 외래 키가 일기 삭제를 방해할 수 있으므로 이전 환경에서는 이 단계가 필요합니다.
4. 새 애플리케이션을 시작하고 본인 계정으로 작성·완료·삭제 및 일기 저장을 확인합니다.

MySQL용 SQL (먼저 `SELECT DATABASE()` 결과를 확인):

```sql
SELECT DATABASE();

CREATE TABLE IF NOT EXISTS todo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    content VARCHAR(255) NOT NULL,
    completed BIT NOT NULL,
    register_time DATETIME(6) NULL,
    update_time DATETIME(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_todo_member_id (member_id, id),
    INDEX idx_todo_member_completed_id (member_id, completed, id),
    CONSTRAINT fk_todo_member FOREIGN KEY (member_id) REFERENCES member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 사용자 승인 범위: 기존 일기에 속해 있던 할 일만 삭제.
DROP TABLE IF EXISTS diary_todo;
```

새 DB에서는 JPA 스키마 생성 설정을 사용할 수 있습니다. 기존 DB는 위 전환을 먼저 실행해야 하며, 실행한 뒤 옛 애플리케이션 버전으로 되돌리면 옛 할 일 기능은 복원되지 않습니다. 서버 DB에는 이 개발 작업 중 변경을 실행하지 않았습니다.
