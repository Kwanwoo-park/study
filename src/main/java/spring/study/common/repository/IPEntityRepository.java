package spring.study.common.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.common.entity.IPEntity;

import java.util.List;

@Repository
public interface IPEntityRepository extends JpaRepository<IPEntity, Long> {
    IPEntity findByIp(String ip);

    List<IPEntity> findByMemberId(Long memberId);

    Boolean existsByIp(String ip);

    Boolean existsByIpAndMemberId(String ip, Long memberId);

    Boolean existsByMemberId(Long memberId);

    @Transactional
    void deleteByMemberId(Long memberId);

    @Transactional
    void deleteByIp(String ip);
}
