package spring.study.admin.service;

import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import spring.study.admin.dto.AdminFileResponseDto;
import spring.study.admin.entity.AdminFile;
import spring.study.admin.repository.AdminFileRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminFileService {
    private final AdminFileRepository repository;
    private final TransactionTemplate transactionTemplate;
    private final AdminFileS3Storage storage;
    private final long maxFileSize;

    public AdminFileService(AdminFileRepository repository, PlatformTransactionManager transactionManager, MultipartProperties multipartProperties, AdminFileS3Storage storage) {
        this.repository = repository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.storage = storage;
        long fileLimit = multipartProperties.getMaxFileSize().toBytes();
        long requestLimit = multipartProperties.getMaxRequestSize().toBytes();
        // Even if servlet limits are disabled, this endpoint retains a finite streaming limit.
        this.maxFileSize = Math.min(fileLimit < 0 ? Long.MAX_VALUE : fileLimit, requestLimit < 0 ? Long.MAX_VALUE : requestLimit);
        if (maxFileSize == Long.MAX_VALUE || maxFileSize <= 0) {
            throw new IllegalArgumentException("관리자 파일 업로드에는 양수의 multipart 용량 제한이 필요합니다");
        }
    }

    public long maxFileSize() {
        return maxFileSize;
    }

    public Page<AdminFileResponseDto> list(int page) {
        if (page < 0 || page > 1000000) throw new IllegalArgumentException("올바른 페이지 번호를 입력해 주세요");
        return repository.findAll(PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"))).map(AdminFileResponseDto::from);
    }

    public AdminFileResponseDto upload(MultipartFile file, Long uploadedBy) {
        if (file == null) throw new IllegalArgumentException("업로드할 파일을 선택해 주세요");
        String originalFilename = safeFilename(file.getOriginalFilename());
        if (file.getSize() > maxFileSize) throw tooLarge();
        String id = UUID.randomUUID().toString();
        boolean uploaded = false;
        try {
            // Validate the actual length before S3 PUT without retaining the file in memory or local storage.
            long size = validateStream(file);
            try (InputStream input = file.getInputStream()) {
                storage.upload(id, originalFilename, size, input);
                uploaded = true;
            }
            AdminFile metadata = new AdminFile(id, originalFilename, size, uploadedBy);
            // Complete the commit here so commit failures also remove the S3 object.
            return transactionTemplate.execute(status -> AdminFileResponseDto.from(repository.saveAndFlush(metadata)));
        } catch (IOException error) {
            if (uploaded) storage.removeFailedUpload(id);
            throw new IllegalStateException("업로드 파일을 읽지 못했습니다", error);
        } catch (RuntimeException error) {
            if (uploaded) storage.removeFailedUpload(id);
            throw error;
        }
    }

    public Download download(String id) {
        validateId(id);
        AdminFile metadata = repository.findById(id).orElseThrow(this::notFound);
        return new Download(AdminFileResponseDto.from(metadata), storage.download(id));
    }

    private long validateStream(MultipartFile file) throws IOException {
        long size = 0;
        try (InputStream input = file.getInputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                size += read;
                if (size > maxFileSize) throw tooLarge();
            }
        }
        if (size != file.getSize()) throw new IllegalArgumentException("파일 크기가 일치하지 않습니다. 파일을 다시 선택해 주세요");
        return size;
    }

    private void validateId(String id) {
        if (id == null || !id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) throw notFound();
    }

    private String safeFilename(String filename) {
        if (filename == null) throw new IllegalArgumentException("파일 이름이 필요합니다");
        String normalized = filename.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}\\p{Cf}]", "_").strip();
        if (name.isBlank() || name.equals(".") || name.equals("..") || name.length() > 255) {
            throw new IllegalArgumentException("파일 이름은 1~255자로 지정해 주세요");
        }
        return name;
    }

    private ResponseStatusException tooLarge() {
        return new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "업로드 가능한 파일 용량을 초과하였습니다");
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다");
    }

    public record Download(AdminFileResponseDto metadata, Resource resource) {
    }
}
