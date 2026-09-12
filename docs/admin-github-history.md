# 관리자 GitHub 변경 내역

관리자 메인(`/admin/administrator`)의 **GitHub 내역** 버튼으로 `/admin/github`에 진입합니다. 최근 커밋과 Push 기록을 별도로 조회하며 일반 화면에는 진입 버튼을 추가하지 않습니다.

## 접근과 API

- 페이지와 API 모두 기존 JWT 인증을 사용하고 `ADMIN` 권한을 요구합니다. 비로그인/일반 회원의 페이지 접근은 404, API 접근은 각각 401/403입니다. URL 직접 호출도 권한 검사 대상입니다.
- `GET /api/admin/github/commits?page=1`: 설정한 브랜치의 커밋 메시지, 작성자, 작성 시각, 커미터 시각, SHA 및 상세 링크. 페이지는 1~1000입니다.
- `GET /api/admin/github/activity?cursor=`: Push/강제 Push/병합/브랜치 변경 활동과 작업자, 시각, 전후 SHA, 비교 링크. `nextCursor`를 수정하지 않고 다음 요청의 `cursor`로 전달합니다.
- 응답은 `repository`, `branch`, `entries`, `nextCursor`, `fetchedAt` 구조입니다. 마지막 페이지의 `nextCursor`는 `null`입니다. 각 페이지는 최대 20건입니다.
- 화면과 응답은 `Cache-Control: no-store`이며, 서버 내부에는 성공 결과를 최대 60초·100페이지 보관합니다. 새로고침은 첫 페이지로 돌아가며 캐시가 유효하면 재사용합니다. 자동 polling/알림/배포/DB 저장은 하지 않습니다.

"관리자 페이지에서 접근"은 관리자 영역에만 화면·진입점을 두고 서버에서 권한을 검증한다는 의미입니다. 위조 가능한 Referer 헤더를 인증 수단으로 사용하지 않습니다. 인증된 관리자는 API를 직접 호출할 수도 있습니다.

## 설정

기본 조회 대상은 `Kwanwoo-park/study`의 `main`입니다. 다른 저장소/브랜치로 변경할 때만 서버 환경 변수 또는 배포 속성을 설정하고 재시작합니다. 클라이언트는 조회 대상을 임의로 바꿀 수 없습니다.

```properties
admin.github.owner=Kwanwoo-park
admin.github.repository=study
admin.github.branch=main
admin.github.token=${ADMIN_GITHUB_TOKEN:}
```

공개 저장소는 토큰 없이 조회 가능합니다. 비공개 저장소 또는 인증된 API 한도가 필요하면 해당 저장소의 **Contents: Read-only** 권한을 가진 fine-grained PAT 등을 서버의 `ADMIN_GITHUB_TOKEN` 환경 변수로 주입합니다. 토큰을 HTML/JS/Git에 넣지 않습니다. Docker Compose 사용 시 호스트 환경 변수가 자동으로 컨테이너에 전달되는 것은 아니므로 배포 환경에서 해당 변수를 별도로 전달해야 합니다. 기본 `.properties` 파일은 현재 Git 제외 대상입니다.

GitHub HTTPS API만 호출하고 3초 연결/5초 읽기 제한을 적용합니다. 리다이렉트에 토큰을 전달하지 않습니다. 오류 원문과 인증 헤더는 사용자에게 반환하지 않습니다. GitHub 인증·권한·연결 오류는 API에서 503과 안내 메시지로 반환하며, 조회 제한에 도달하면 Retry-After/리셋 시각에 따라 후속 외부 조회를 잠시 멈춥니다.

## 내역의 의미

커밋 작성·커미터 시각은 Push 시각과 다릅니다. 로컬에만 있고 Push하지 않은 커밋은 조회되지 않습니다. Push 목록은 GitHub의 저장소 Activity API가 제공하는 기록이며 영구 감사 로그나 배포 완료 내역이 아닙니다. 화면의 모든 시각은 한국 시간(KST)으로 표시합니다.

- [GitHub 커밋 API](https://docs.github.com/en/rest/commits/commits#list-commits)
- [GitHub 저장소 활동 API](https://docs.github.com/en/rest/repos/repos#list-repository-activities)

테스트는 GitHub 응답을 모의하므로 실제 운영 토큰 권한, 저장소 접근 가능 여부 및 서버의 외부 통신은 배포 환경에서 확인해야 합니다.
