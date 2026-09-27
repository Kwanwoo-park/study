package spring.study.appeal.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import spring.study.appeal.dto.AppealRequestDto;
import spring.study.appeal.dto.AppealResponseDto;
import spring.study.appeal.dto.AppealSanctionResponseDto;
import spring.study.appeal.entity.Appeal;
import spring.study.appeal.entity.AppealStatus;
import spring.study.appeal.repository.AppealRepository;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.event.MemberChangedEvent;
import spring.study.member.repository.MemberRepository;
import spring.study.member.sanction.entity.MemberSanction;
import spring.study.member.sanction.repository.MemberSanctionRepository;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AppealService {
    private static final int MAX_PAGE_SIZE = 100;

    private final AppealRepository appealRepository;

    @Transactional
    public Appeal create(AppealRequestDto requestDto, Member member, MemberSanction sanction) {
        if (appealRepository.existsByMemberAndStatus(member, AppealStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 검토 대기 중인 상소문이 있습니다");
        }

        Appeal appeal = appealRepository.save(Appeal.builder()
                .member(member)
                .relatedSanction(sanction)
                .title(requestDto.getTitle().trim())
                .content(requestDto.getContent().trim())
                .build());

        return appeal;
    }


    @Transactional(readOnly = true)
    public List<AppealResponseDto> findByMember(Member member) {
        return appealRepository.findByMemberOrderByRegisterTimeDesc(member).stream()
                .map(AppealResponseDto::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AppealResponseDto> findAll(AppealStatus status, int page, int size) {
        AppealStatus resolvedStatus = status == null ? AppealStatus.PENDING : status;
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Order.desc("registerTime"), Sort.Order.desc("id"))
        );
        return appealRepository.findByStatus(resolvedStatus, pageable).map(AppealResponseDto::new);
    }

    @Transactional
    public Appeal accept(Long appealId) {
        Appeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상소문을 찾을 수 없습니다"));
        if (appeal.getStatus() != AppealStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 처리가 완료된 상소문입니다");
        }

        appeal.accept();
        return appeal;
    }

    @Transactional
    public void deleteByMember(Member member) {
        appealRepository.deleteByMember(member);
    }






}
