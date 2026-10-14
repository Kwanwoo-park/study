package spring.study.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import spring.study.admin.entity.AdminFile;

import java.util.Optional;

public interface AdminFileRepository extends JpaRepository<AdminFile, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AdminFile> findFirstByOriginalFilenameOrderByCreatedAtDescIdDesc(String originalFilename);
}
