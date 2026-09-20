package spring.study.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
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
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFileService {
    private final AdminFileRepository adminFileRepository;
    private final PlatformTransactionManager transactionManager;
    private final AdminFileS3Storage storage;

    @Transactional(readOnly = true)
    public Page<AdminFileResponseDto> list(int page) {
        if (page < 0 || page > 1000000) throw new IllegalArgumentException("올바른 페이지 번호를 입력해 주세요");
        return adminFileRepository.findAll(PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"))).map(AdminFileResponseDto::from);
    }

    public AdminFileResponseDto upload(MultipartFile file, Long uploadedBy) {
        if (file == null) throw new IllegalArgumentException("업로드할 파일을 선택해 주세요");
        String originalFilename = safeFilename(file.getOriginalFilename());
        // The servlet has already applied multipart limits before this service is called.
        long size = file.getSize();
        String id = UUID.randomUUID().toString();
        boolean uploaded = false;
        try {
            try (InputStream input = file.getInputStream()) {
                storage.upload(id, originalFilename, size, input);
                uploaded = true;
            }
            AdminFile metadata = new AdminFile(id, originalFilename, size, uploadedBy);
            // Complete the commit here so commit failures also remove the S3 object.
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            return transactionTemplate.execute(status -> AdminFileResponseDto.from(adminFileRepository.saveAndFlush(metadata)));
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
        AdminFile metadata = adminFileRepository.findById(id).orElseThrow(this::notFound);
        return new Download(AdminFileResponseDto.from(metadata), storage.download(id));
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

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다");
    }

    public record Download(AdminFileResponseDto metadata, Resource resource) { }
}
