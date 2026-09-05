# 관리자 파일 S3 배포

관리자 홈의 **File Storage** 또는 `/admin/files`에서 모든 형식의 파일을 S3에 업로드하고, 목록 조회 및 다운로드할 수 있습니다. 화면과 API는 모두 `ADMIN` 권한을 요구합니다. 기존 이미지 업로드의 버킷·공개 범위·확장자 제한은 변경하지 않습니다.

## 1. 별도 비공개 버킷 준비

- 기존 `cloud.aws.region.static`과 같은 리전에 관리자 파일용 **일반 S3 버킷**을 준비합니다. 클라이언트와 AWS 인증은 기존 `S3Config`의 설정을 재사용합니다.
- 새 버킷의 **권한 → 퍼블릭 액세스 차단**에서 4개 항목을 모두 켭니다. 코드는 업로드/다운로드 때마다 이 버킷 수준 설정을 확인하며, 미설정·권한 부족·네트워크 오류가 있으면 파일 전송을 거부합니다. 계정 수준에서만 차단한 구성은 이 구현에서 허용하지 않습니다.
- **객체 소유권: 버킷 소유자 적용(ACL 비활성화)** 구성을 사용할 수 있습니다. 업로드 요청에 공개 ACL이나 별도 ACL을 지정하지 않습니다.
- 관리자 파일 버킷은 **버전 관리 비활성화**를 전제로 사용합니다. 버전 ID를 저장하거나 특정 버전으로 읽기/삭제하는 기능은 사용하지 않습니다.
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
      "Action": "s3:GetBucketPublicAccessBlock",
      "Resource": "arn:aws:s3:::your-private-admin-file-bucket"
    },
    {
      "Sid": "StoreAndReadAdminFiles",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::your-private-admin-file-bucket/admin-files/*"
    }
  ]
}
```

`GetBucketPublicAccessBlock`은 비공개 설정 검사에 필요합니다. [AWS API 권한 안내](https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetPublicAccessBlock.html)

삭제 권한은 S3 업로드 후 DB 저장/커밋 실패 시 설정된 관리자 버킷의 `admin-files/{id}` 객체를 정리하기 위한 것입니다. 버전별 접근 권한은 필요하지 않습니다. 사용자용 파일 삭제 기능을 추가한 것은 아닙니다.

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

용량은 기존 `spring.servlet.multipart.max-file-size`, `spring.servlet.multipart.max-request-size`를 따릅니다. 실행 파일 때문에 상한을 늘려야 할 때만 배포 설정을 수정합니다. 요청 전체 제한에는 multipart 부가 데이터도 포함됩니다.

```properties
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=110MB
```

이 설정은 다른 업로드 API에도 적용됩니다. Nginx 등 프록시가 있으면 요청 용량과 전송 시간 제한도 함께 확인하세요. 영구 파일은 S3에만 저장하지만, multipart 요청을 처리하는 동안 서블릿 임시 파일이 사용될 수 있습니다.

## 4. DB 적용 및 중복 컬럼 제거

`admin_file`에는 `id`, `original_filename`, `size`, `uploaded_by`, `created_at`만 저장합니다. 파일은 항상 설정된 관리자 버킷의 `admin-files/{id}`로 조회합니다. 기존 이미지/관리자 버킷 설정과 UUID 경로를 유지하면 이미 업로드한 S3 파일을 옮길 필요가 없습니다.

- 신규 DB: `ddl-auto=update`로 테이블을 생성하거나, `validate`/`none` 환경에서는 `AdminFile` 엔티티에 맞춰 스키마를 직접 준비합니다. 별도 SQL 스크립트는 제공하지 않습니다.
- 기존 DB: `ddl-auto=update`는 사용하지 않는 컬럼을 자동 삭제하지 않습니다. 아래 사전 확인 후 DB에서 `s3_bucket`, `s3_key`, `s3_version_id`만 직접 제거합니다. 파일 행·실제 S3 객체는 삭제하지 않습니다.
- 제거 전 DB를 백업하고, 아래 `your-private-admin-file-bucket`을 **현재 관리자 버킷 이름**으로 바꿔 조회합니다. 조회 결과가 있으면 과거 버킷/경로/버전 또는 로컬 저장 데이터이므로 먼저 저장 위치를 확인해야 합니다. 애플리케이션이 이 조건을 자동 검사하거나 컬럼을 삭제하지 않습니다.

```sql
SELECT id, s3_bucket, s3_key, s3_version_id
FROM admin_file
WHERE s3_bucket IS NULL
   OR s3_bucket <> 'your-private-admin-file-bucket'
   OR s3_key IS NULL
   OR s3_key <> CONCAT('admin-files/', id)
   OR (s3_version_id IS NOT NULL AND s3_version_id <> 'null');
```

이제 파일별 저장 위치를 DB에 남기지 않으므로 관리자 버킷 설정이나 `admin-files/` 경로를 바꾸면 기존 파일을 찾을 수 없게 됩니다. 변경이 필요할 때는 파일 이관을 별도로 진행해야 합니다. 이전 로컬 파일/행은 자동 삭제하지 않으며, S3에 없는 파일은 원본으로 다시 업로드해야 합니다.

DB와 S3의 백업/보존 정책을 함께 관리하세요. DB 실패 시 S3 객체 정리를 시도하지만, 강제 종료·응답 유실·삭제 권한 문제로 DB에 연결되지 않은 객체가 남을 수 있습니다. 이 경우 서버 로그의 버킷/키를 기준으로 DB의 ID와 진행 중 업로드를 확인한 후 정리해야 합니다. 파일 전체를 자동 만료시키는 S3 수명 주기 정책은 활성 파일도 지울 수 있으므로 이 목적에 그대로 적용하지 마세요.

## 5. 배포 후 확인

1. 관리자 계정으로 `/admin/files`에 들어가 작은 `.exe`, `.zip`, 확장자 없는 파일을 업로드합니다.
2. 설정된 관리자 버킷의 `admin-files/{id}` 객체와 DB의 ID·파일명·크기를 확인합니다. 변경 전에 업로드한 파일도 다운로드되며 원본 바이너리와 같은지 확인합니다.
3. 로그아웃/일반 회원 상태에서 목록·업로드·다운로드 API가 거부되는지 확인합니다. CSRF 없는 업로드도 거부되어야 합니다.
4. 로그인하지 않은 별도 브라우저에서 **서명 없는 S3 객체 URL**로 파일을 내려받을 수 없는지 확인합니다. 공개 URL이나 presigned URL을 발급하는 기능은 제공하지 않습니다.

`401`은 로그인 필요, `403`은 관리자 권한 또는 CSRF 문제, `413`은 업로드 용량 초과입니다. `503`은 버킷 미설정, 공개 접근 차단 검사 실패, IAM/KMS 또는 S3 통신 문제이므로 응답 안내와 서버 로그를 확인하세요. 객체가 없는 경우 S3가 `NoSuchKey`를 반환하면 `404`로 변환합니다. S3 읽기 권한 오류를 파일이 없는 것으로 처리하지 않습니다.

테스트는 실제 AWS 버킷에 쓰지 않고 S3 클라이언트를 모의하여 수행합니다. 실제 버킷 생성·IAM 적용·네트워크 통신 검증은 배포 환경에서 별도로 진행해야 합니다.
