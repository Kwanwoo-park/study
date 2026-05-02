package spring.study.collection.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import spring.study.collection.dto.CollectionRequestDto;
import spring.study.collection.dto.CollectionResponseDto;
import spring.study.collection.entity.Collection;
import spring.study.collection.repository.CollectionRepository;
import spring.study.member.entity.Member;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CollectionService {
    private final CollectionRepository collectionRepository;

    @Transactional
    public Collection save(Collection collection) {
        return collectionRepository.save(collection);
    }

    @Transactional
    public Collection save(CollectionRequestDto dto) {
        return collectionRepository.save(dto.toEntity());
    }

    public List<CollectionResponseDto> getCollections(int cursor, int limit, Member member) {
        return collectionRepository.findByMember(member, PageRequest.of(cursor, limit, Sort.by("registerTime").descending()))
                .stream().map(CollectionResponseDto::new).toList();
    }

    public List<CollectionResponseDto> findAll() {
        return collectionRepository.findAll().stream().map(CollectionResponseDto::new).toList();
    }

    public Collection findById(Long id) {
        return collectionRepository.findById(id).orElseThrow();
    }

    public Long countByMember(Member member) {
        return collectionRepository.countByMember(member);
    }

    @Transactional
    public void deleteById(Long id) {
        collectionRepository.deleteById(id);
    }

    @Transactional
    public void deleteByIds(List<Long> ids) {
        collectionRepository.deleteByIdIn(ids);
    }

    public void deleteByMember(Member member) {
        collectionRepository.deleteByMember(member);
    }
}
