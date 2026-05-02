package spring.study.collection.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.collection.entity.Collection;
import spring.study.member.entity.Member;

import java.util.List;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, Long> {
    List<Collection> findByMember(Member member, Pageable pageable);

    long countByMember(Member member);

    @Transactional
    void deleteByIdIn(List<Long> idList);

    @Transactional
    void deleteByMember(Member member);
}
