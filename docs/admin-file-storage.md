# 관리자 파일 S3 배포

관리자 홈의 **File Storage** 또는 `/admin/files`에서 모든 형식의 파일을 S3에 업로드하고, 목록 조회 및 다운로드할 수 있습니다. 화면과 API는 모두 `ADMIN` 권한을 요구합니다. 기존 이미지 업로드의 버킷·공개 범위·확장자 제한은 변경하지 않습니다.

## 1. 별도 비공개 버킷 준비

- 기존 `cloud.aws.region.static`과 같은 리전에 관리자 파일용 **일반 S3 버킷**을 준비합니다. 클라이언트와 AWS 인증은 기존 `S3Config`의 설정을 재사용합니다.
- 새 버킷의 **권한 → 퍼블릭 액세스 차단**에서 4개 항목을 모두 켭니다. 코드는 업로드/다운로드 때마다 이 버킷 수준 설정을 확인하며, 미설정·권한 부족·네트워크 오류가 있으면 파일 전송을 거부합니다. 계정 수준에서만 차단한 구성은 이 구현에서 허용하지 않습니다.
- **객체 소유권: 버킷 소유자 적용(ACL 비활성화)** 구성을 사용할 수 있습니다. 업로드 요청에 공개 ACL이나 별도 ACL을 지정하지 않습니다.
- 관리자 파일 버킷의 **버전 관리(Versioning)를 Enabled로 설정**합니다. 코드는 업로드 전에 활성 상태를 확인하며 비활성화·일시 중지 상태에서는 업로드를 거부합니다. 같은 파일명은 기존 UUID 키에 새 버전으로 업로드하고, 이전 버전은 유지합니다. 버전 관리를 새로 활성화했다면 AWS 안내에 따라 전파 시간을 확보한 뒤 업로드하세요. [AWS Versioning 안내](https://docs.aws.amazon.com/AmazonS3/latest/userguide/versioning-workflows.html)
- 퍼블릭 읽기 정책, 공개 웹사이트, 공개 CloudFront 배포 또는 별도 파일 공유 서비스에 연결하지 마세요. 애플리케이션 권한 검사와 별개로 AWS IAM 권한을 가진 주체는 S3에 접근할 수 있으므로, 해당 권한도 서버 운영 주체로 제한해야 합니다.

버킷 이름을 숨기는 방식에 의존하지 않습니다. S3 공개 접근 차단은 버킷/계정 수준의 권한 제어이며, 이미지용 버킷에 새 폴더를 만드는 것만으로 파일이 비공개가 되지는 않습니다. [AWS 공개 접근 차단 설명](https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-control-block-public-access.html)

## 2. 서버 IAM 권한

기존 AWS 인증 정보가 가리키는 IAM 주체에 다음 정책을 **추가**합니다. 기존 이미지용 권한을 교체하지 마세요. 아래 `your-private-admin-file-bucket`은 실제 새 버킷 이름으로 변경합니다.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "CheckAdminFileBucketPrivacy",
      "Effect": "Allow",
      "Action": ["s3:GetBucketPublicAccessBlock", "s3:GetBucketVersioning"],
      "Resource": "arn:aws:s3:::your-private-admin-file-bucket"
    },
    {
      "Sid": "StoreAndReadAdminFiles",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:GetObjectVersion",
        "s3:DeleteObjectVersion"
      ],
      "Resource": "arn:aws:s3:::your-private-admin-file-bucket/admin-files/*"
    }
  ]
}
```

`GetBucketPublicAccessBlock`은 비공개 설정 검사, `GetBucketVersioning`은 업로드 전 버전 관리 검사에 필요합니다. [공개 접근 차단 API](https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetPublicAccessBlock.html), [버전 관리 조회 API](https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketVersioning.html)

`GetObjectVersion`은 DB에 저장된 버전의 다운로드, `DeleteObjectVersion`은 DB 저장/롤백 시 이번 업로드의 버전만 정리하기 위한 권한입니다. 기존 UUID 키 전체를 삭제하는 `DeleteObject`는 사용하지 않으며, 이전 버전은 삭제하지 않습니다. 사용자용 파일 삭제 기능을 추가한 것은 아닙니다. [AWS 버전 삭제 안내](https://docs.aws.amazon.com/AmazonS3/latest/userguide/DeletingObjectVersions.html)

버킷 기본 암호화가 SSE-KMS라면 KMS 키 정책과 서버의 `kms:GenerateDataKey`, `kms:Decrypt` 권한도 필요합니다. 버킷 정책에 별도의 업로드 조건이 있으면 해당 조건도 확인하세요. 코드는 버킷의 기본 암호화 설정을 따릅니다. [AWS PutObject 권한 안내](https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html)

## 3. 애플리케이션 설정

Docker Compose에서는 `.env`의 다음 값을 컨테이너 환경 변수로 전달합니다. 값이 비어 있으면 관리자 업로드가 비활성 상태로 남으며 기존 이미지 버킷으로 대체하지 않습니다.

```dotenv
ADMIN_FILES_S3_BUCKET=your-private-admin-file-bucket
```

JAR/IDE에서 직접 실행한다면 위 환경 변수 또는 아래 배포 속성 중 하나를 사용합니다. Docker Compose에서는 환경 변수 값이 속성 파일보다 우선하므로 `.env`에 설정하세요.

```properties
admin.files.s3.bucket=your-private-admin-file-bucket
```

`cloud.aws.s3.bucketName`은 **기존 이미지 버킷 이름 그대로** 둡니다. Access Key/Secret Key를 프론트엔드나 저장소에 추가하지 마세요. 이 기능에 관리자 파일용 로컬 경로·볼륨 설정은 필요하지 않습니다.

관리자 파일과 일반 이미지(게시글·채팅·다이어리·프로필·컬렉션)는 모두 Spring 공통 설정으로 **파일당 20MB, 요청 전체 210MB**를 적용합니다. 용량 제한은 서블릿의 multipart 파싱 단계에서 검사하며, 관리자 서비스는 `MultipartFile.getSize()`를 S3와 DB의 파일 크기로 사용하고 업로드 스트림을 한 번만 엽니다. 관리자 화면의 상한 표시에는 `spring.servlet.multipart.max-file-size`를 사용합니다. 별도의 `admin.files.max-file-size` 설정과 서비스의 중복 용량·스트림 길이 검사는 사용하지 않습니다.

```properties
spring.servlet.multipart.max-file-size=20MB
spring.servlet.multipart.max-request-size=210MB
```

Spring의 `MB` 단위는 1,048,576바이트이므로 파일당 20,971,520바이트, 요청 전체 220,200,960바이트입니다. 이미지 최대 10장 정책은 유지되므로 20MB 이미지 10장의 200MB에 multipart 부가 데이터 여유를 더한 요청 상한입니다. 요청 전체 제한에는 multipart 경계·파일 이름·폼 필드도 포함됩니다.

배포 서버의 설정을 변경한 뒤 재시작하세요. `application.properties`는 Git 제외 파일이므로 코드 업데이트만으로 서버의 설정이 바뀌지 않습니다. 기존 `admin.files.max-file-size`는 제거하고, 환경 변수 `SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE` / `SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE`가 이전 값으로 공통 설정을 덮어쓰지 않는지도 확인하세요. Nginx 등 프록시가 있으면 `client_max_body_size`와 전송 시간 제한도 함께 확인하세요. 영구 파일은 S3에만 저장하지만, multipart 요청을 처리하는 동안 서블릿 임시 파일이 사용될 수 있습니다.

## 4. DB 적용과 같은 파일명 재업로드

`admin_file`에는 `id`, `original_filename`, `size`, `uploaded_by`, `created_at`, `s3_version_id`를 저장합니다. 파일 키는 설정된 관리자 버킷의 `admin-files/{id}`이며 `s3_version_id`는 DB에 정상 저장된 다운로드 대상 버전입니다. 파일 내용이나 버전별 이력 테이블은 DB에 추가하지 않습니다.

- 신규 DB: `ddl-auto=update`로 테이블을 생성하거나 `AdminFile` 엔티티에 맞춰 스키마를 준비합니다.
- 기존 DB: `s3_version_id` 컬럼은 삭제하지 마세요. 컬럼이 없다면 `ddl-auto=update`로 추가하거나 아래 DDL을 배포 전에 한 번 적용합니다. 이미 있다면 자료형/길이/nullable 설정만 확인하고, 값을 지우지 마세요.

```sql
-- s3_version_id 컬럼이 없는 경우에만 실행
ALTER TABLE admin_file ADD COLUMN s3_version_id VARCHAR(1024) NULL;
```

- 같은 파일명은 DB 조회로 찾은 UUID를 재사용하고 크기·업로더·버전 ID만 갱신합니다. 최초 생성 시각은 유지합니다. 파일명 비교는 DB의 문자열 비교 규칙을 따릅니다.
- 기존에 같은 이름의 행이 여러 개 있다면 최초 생성 시각이 가장 최근인 행(ID 내림차순으로 동률 구분)을 재사용합니다. 나머지 행과 S3 파일은 자동 삭제·병합하지 않습니다.
- 기존 행의 버전 ID가 비어 있으면 재업로드 전에 S3의 현재 버전을 확인하고 별도 트랜잭션으로 DB에 먼저 고정합니다. Versioning 활성화 전에 생성된 객체는 문자열 `"null"` 버전을 지정합니다. 이 단계가 실패하면 새 파일은 업로드하지 않습니다.
- 기존 행은 DB 잠금으로 재업로드를 직렬화합니다. 서로 동시에 최초 업로드한 같은 이름은 별도 UUID로 등록될 수 있으며, 이후에는 위 조회 규칙으로 재사용합니다.
- 업로드 후 정상 반환된 S3 버전 ID를 DB에 저장합니다. 저장 중 실패하거나 트랜잭션이 롤백되면 **이번에 생성된 버전만** 삭제합니다. 다운로드는 S3의 무조건 최신 버전이 아니라 DB에 고정된 버전을 읽으므로 이전 정상 버전은 그대로 유지됩니다.
- 기존 버전 목록 확인/복원은 현재 S3 콘솔에서 진행합니다. 콘솔에서 다른 버전을 최신으로 만들어도 애플리케이션의 고정 버전은 자동 변경되지 않습니다. 버전 이력 UI/API는 이번 변경에 포함하지 않습니다.

관리자 버킷 설정이나 `admin-files/` 경로를 바꾸면 기존 파일을 찾을 수 없게 됩니다. 기존 파일을 옮길 필요는 없지만, 저장 위치 변경이 필요할 때는 별도로 이관해야 합니다. 이전 로컬 파일/행은 자동 삭제하지 않습니다.

DB와 S3의 백업/보존 정책을 함께 관리하세요. 강제 종료·업로드 응답 유실·삭제 권한 문제로 DB에 연결되지 않은 버전이 남을 수 있습니다. 버전 ID를 확인하지 못했거나 DB 커밋 결과가 불명확하면 기존 파일 보호를 위해 키 전체를 삭제하지 않습니다. 서버 로그와 DB의 ID/버전 ID를 확인한 후 실패 버전만 정리하세요. DB가 가리키는 버전은 S3의 비최신 버전일 수도 있으므로 전체 객체나 비최신 버전을 무조건 만료시키는 수명 주기 정책을 적용하지 마세요.

## 5. 배포 후 확인

1. 관리자 계정으로 `/admin/files`에 들어가 작은 `.exe`, `.zip`, 확장자 없는 파일을 업로드합니다.
2. 설정된 관리자 버킷의 `admin-files/{id}` 객체와 DB의 ID·파일명·크기를 확인합니다. 변경 전에 업로드한 파일도 다운로드되며 원본 바이너리와 같은지 확인합니다.
   같은 이름으로 내용이 다른 파일을 다시 올려 UUID와 DB 행 수가 유지되고 `s3_version_id`와 다운로드 내용이 갱신되는지 확인합니다. S3 콘솔에서는 이전 버전도 남아 있어야 합니다.
3. 로그아웃/일반 회원 상태에서 목록·업로드·다운로드 API가 거부되는지 확인합니다. CSRF 없는 업로드도 거부되어야 합니다.
4. 로그인하지 않은 별도 브라우저에서 **서명 없는 S3 객체 URL**로 파일을 내려받을 수 없는지 확인합니다. 공개 URL이나 presigned URL을 발급하는 기능은 제공하지 않습니다.

`401`은 로그인 필요, `403`은 관리자 권한 또는 CSRF 문제, `413`은 업로드 용량 초과입니다. `503`은 버킷 미설정, 공개 접근 차단 검사 실패, IAM/KMS 또는 S3 통신 문제이므로 응답 안내와 서버 로그를 확인하세요. 객체가 없는 경우 S3가 `NoSuchKey`를 반환하면 `404`로 변환합니다. S3 읽기 권한 오류를 파일이 없는 것으로 처리하지 않습니다.

테스트는 실제 AWS 버킷에 쓰지 않고 S3 클라이언트를 모의하여 수행합니다. 실제 버킷 생성·IAM 적용·네트워크 통신 검증은 배포 환경에서 별도로 진행해야 합니다.
