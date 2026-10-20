package spring.study.appeal.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import spring.study.appeal.dto.AppealRequestDto;
import spring.study.appeal.dto.AppealResponseDto;
import spring.study.appeal.entity.Appeal;
import spring.study.appeal.entity.AppealStatus;
import spring.study.appeal.repository.AppealRepository;
import spring.study.member.entity.Member;
import spring.study.member.sanction.entity.MemberSanction;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppealService {
    private final AppealRepository appealRepository;

    @Transactional
    public Appeal create(AppealRequestDto requestDto, Member member, MemberSanction sanction) {
        if (appealRepository.existsByMemberAndStatus(member, AppealStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 검토 대기 중인 상소문이 있습니다");
        }

        return appealRepository.save(Appeal.builder()
                .member(member)
                .relatedSanction(sanction)
                .title(requestDto.getTitle().trim())
                .content(requestDto.getContent().trim())
                .build());
    }


    @Transactional(readOnly = true)
    public List<AppealResponseDto> findByMember(Member member) {
        return appealRepository.findByMemberOrderByRegisterTimeDesc(member).stream().map(AppealResponseDto::new).toList();
    }

    @Transactional(readOnly = true)
    public Page<AppealResponseDto> findAll(AppealStatus status, int page, int size) {
        AppealStatus resolvedStatus = status == null ? AppealStatus.PENDING : status;

        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Order.desc("registerTime"), Sort.Order.desc("id"))
        );

        return appealRepository.findByStatus(resolvedStatus, pageable).map(AppealResponseDto::new);
    }

    @Transactional
    public Appeal changeStatus(Long appealId, AppealStatus status) {
        Appeal appeal = appealRepository.findById(appealId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상소문을 찾을 수 없습니다"));

        if (appeal.getStatus() != AppealStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 처리가 완료된 상소문입니다");
        }

        appeal.changeStatus(status);

        return appeal;
    }

    @Transactional
    public void deleteByMember(Member member) {
        appealRepository.deleteByMember(member);
    }

    @Transactional
    public void detachSanctions(Member member) {
        appealRepository.detachSanctions(member);
    }






}
