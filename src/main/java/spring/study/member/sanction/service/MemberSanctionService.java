package spring.study.member.sanction.service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import spring.study.member.entity.Member;
import spring.study.member.event.MemberChangedEvent;
import spring.study.member.sanction.entity.MemberSanction;
import spring.study.member.sanction.repository.MemberSanctionRepository;
import spring.study.report.dto.ReportProcessRequestDto;
import spring.study.report.entity.Report;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class MemberSanctionService {
    private final MemberSanctionRepository memberSanctionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void deleteByMemberReferences(Member member) {
        memberSanctionRepository.deleteByMemberOrIssuedByOrReportReporter(member, member, member);
    }

    @Transactional
    public void apply(Report report, ReportProcessRequestDto requestDto, Member admin, Member target) {
        if (memberSanctionRepository.existsByReportId(report.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 제재가 적용된 신고입니다");
        }

        switch (requestDto.getAction()) {
            case WARNING -> target.addWarning();
            case TEMPORARY_SUSPEND -> target.suspendUntil(requestDto.getSuspendedUntil());
            case PERMANENT_BAN -> target.ban();
            default -> throw new IllegalStateException("지원하지 않는 회원 제재입니다");
        }
        eventPublisher.publishEvent(new MemberChangedEvent(target.getId()));

        memberSanctionRepository.save(MemberSanction.builder()
                .member(target)
                .report(report)
                .issuedBy(admin)
                .type(requestDto.getAction())
                .reason(requestDto.getReportMemo())
                .startedAt(LocalDateTime.now())
                .expiredAt(requestDto.getSuspendedUntil())
                .build());

    }

    @Transactional(readOnly = true)
    public MemberSanction findOwned(Long id, Member member) {
        if (id == null) return null;
        return memberSanctionRepository.findByIdAndMember(id, member)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인에게 적용된 제재 내역만 선택할 수 있습니다"));
    }

    @Transactional(readOnly = true)
    public List<MemberSanction> findByMember(Member member) {
        return memberSanctionRepository.findByMemberOrderByStartedAtDesc(member);
    }

    @Transactional
    public void unblock(Member member, MemberSanction sanction) {
        if (!member.isAccessBlocked()) throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 차단이 해제된 회원입니다");
        member.activate();
        if (sanction != null) sanction.cancel(LocalDateTime.now());
        eventPublisher.publishEvent(new MemberChangedEvent(member.getId()));
    }
}
