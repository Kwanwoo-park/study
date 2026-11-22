package spring.study.chat.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.study.chat.entity.ChatImageUpload;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatImageUploadRepository extends JpaRepository<ChatImageUpload, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    ChatImageUpload findForUpdateById(String id);

    @Query("select upload.id from ChatImageUpload upload where upload.expiresAt <= :now order by upload.expiresAt, upload.id")
    List<String> findExpiredIds(@Param("now") LocalDateTime now, Pageable pageable);
}
