package spring.study.forbidden.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import spring.study.forbidden.entity.Forbidden;
import spring.study.forbidden.entity.Risk;
import spring.study.forbidden.entity.Status;

import java.util.List;

@Repository
public interface ForbiddenRepository extends JpaRepository<Forbidden, Long> {
    List<Forbidden> findByWord(String word);

    List<Forbidden> findByStatus(Status status);

    List<Forbidden> findByStatusNot(Status status);

    List<Forbidden> findByRisk(Risk risk);

    @Transactional
    @Modifying
    @Query("update forbidden set status = :status where id in (:idList)")
    int updateStatusInIdList(@Param("status") Status status, @Param("idList") List<Long> idList);

    Boolean existsByWord(String word);
}
