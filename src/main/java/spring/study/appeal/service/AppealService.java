package spring.study.appeal.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
import spring.study.member.repository.MemberRepository;
import spring.study.member.sanction.entity.MemberSanction;
import spring.study.member.sanction.repository.MemberSanctionRepository;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppealService {
    private static final int MAX_PAGE_SIZE = 100;

    private final AppealRepository appealRepository;
    private final MemberRepository memberRepository;
    private final MemberSanctionRepository memberSanctionRepository;
    private final AppealVerificationService appealVerificationService;
    private final NotificationService notificationService;

    @Transactional
    public AppealResponseDto create(AppealRequestDto requestDto, Member authenticatedMember) {
        Member member = resolveMember(requestDto, authenticatedMember);
        if (appealRepository.existsByMemberAndStatus(member, AppealStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 검토 대기 중인 상소문이 있습니다");
        }

        MemberSanction sanction = resolveSanction(requestDto.getSanctionId(), member);
        Appeal appeal = appealRepository.save(Appeal.builder()
                .member(member)
                .relatedSanction(sanction)
                .title(requestDto.getTitle().trim())
                .content(requestDto.getContent().trim())
                .build());

        notifyAdministrators(appeal);
        return new AppealResponseDto(appeal);
    }

    public List<AppealSanctionResponseDto> findSanctions(Member member) {
        return memberSanctionRepository.findByMemberOrderByStartedAtDesc(member).stream()
                .map(AppealSanctionResponseDto::new)
                .toList();
    }

    public List<AppealResponseDto> findByMember(Member member) {
        return appealRepository.findByMemberOrderByRegisterTimeDesc(member).stream()
                .map(AppealResponseDto::new)
                .toList();
    }

    public Page<AppealResponseDto> findAll(AppealStatus status, int page, int size) {
        AppealStatus resolvedStatus = status == null ? AppealStatus.PENDING : status;
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Order.desc("registerTime"), Sort.Order.desc("id"))
        );
        return appealRepository.findByStatus(resolvedStatus, pageable).map(AppealResponseDto::new);
    }

    public void deleteByMember(Member member) {
        appealRepository.deleteByMember(member);
    }

    private Member resolveMember(AppealRequestDto requestDto, Member authenticatedMember) {
        if (authenticatedMember != null) {
            return authenticatedMember;
        }
        if (requestDto.getEmail() == null || requestDto.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "비로그인 상소는 이메일 인증이 필요합니다");
        }
        return appealVerificationService.consumeVerification(
                requestDto.getEmail(), requestDto.getVerificationToken());
    }

    private MemberSanction resolveSanction(Long sanctionId, Member member) {
        if (sanctionId == null) {
            return null;
        }
        return memberSanctionRepository.findByIdAndMember(sanctionId, member)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "본인에게 적용된 제재 내역만 선택할 수 있습니다"
                ));
    }

    private void notifyAdministrators(Appeal appeal) {
        String memberName = appeal.getMember().getName() == null || appeal.getMember().getName().isBlank()
                ? appeal.getMember().getEmail()
                : appeal.getMember().getName();
        String message = memberName + "님이 상소문을 제출했습니다.";
        String url = "/admin/appeal?appealId=" + appeal.getId();

        memberRepository.findAllByRole(Role.ADMIN).forEach(admin ->
                notificationService.createNotification(admin, message, Group.ADMIN, url)
        );
    }
}
