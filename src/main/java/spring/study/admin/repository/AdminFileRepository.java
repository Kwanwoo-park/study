package spring.study.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.study.admin.entity.AdminFile;

public interface AdminFileRepository extends JpaRepository<AdminFile, String> {
}
