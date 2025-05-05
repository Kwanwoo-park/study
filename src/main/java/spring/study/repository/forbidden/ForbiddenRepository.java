package spring.study.repository.forbidden;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.entity.forbidden.Forbidden;
import spring.study.entity.forbidden.Risk;
import spring.study.entity.forbidden.Status;

import java.util.List;

@Repository
public interface ForbiddenRepository extends JpaRepository<Forbidden, Long> {
    Forbidden findByWord(String word);

    List<Forbidden> findByStatus(Status status);

    List<Forbidden> findByRisk(Risk risk);

    Boolean existsByWord(String word);
}
