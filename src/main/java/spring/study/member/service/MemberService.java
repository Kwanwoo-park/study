package spring.study.member.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.dto.MemberResponseDto;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.event.MemberChangedEvent;
import spring.study.member.event.MemberDeletedEvent;
import spring.study.common.entity.CommonVisibility;
import spring.study.common.service.VisibilityAccessPolicy;
import spring.study.member.repository.MemberRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService implements UserDetailsService {
    private final MemberRepository memberRepository;
    private final VisibilityAccessPolicy visibilityAccessPolicy;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long save(MemberRequestDto memberSaveDto) {
        return memberRepository.save(memberSaveDto.toEntity()).getId();
    }

    @Transactional
    public Member save(Member member) { return memberRepository.save(member); }

    @Transactional
    public HashMap<String, Object> findAll(Integer page, Integer size) {
        HashMap<String, Object> member = new HashMap<>();

        Page<Member> list = memberRepository.findAll(PageRequest.of(page, size, Sort.by("id").ascending()));

        member.put("list", list.stream().map(MemberResponseDto::new).collect(Collectors.toList()));
        member.put("paging", list.getPageable());
        member.put("totalCnt", list.getTotalElements());
        member.put("totalPage", list.getTotalPages());

        return member;
    }

    public Member findById(Long id) {
        return memberRepository.findById(id).orElseThrow();
    }

    public List<MemberResponseDto> findName(String name) {
        return memberRepository.findByNameContaining(name).stream().map(MemberResponseDto::new).toList();
    }

    public List<MemberResponseDto> findName(String name, Member viewer) {
        return memberRepository.findByNameContaining(name).stream()
                .filter(member -> visibilityAccessPolicy.canViewMember(member, viewer))
                .map(MemberResponseDto::new)
                .toList();
    }

    public List<Member> findMember(List<Long> list) {
        return memberRepository.findByIdIn(list);
    }

    public Member findMember(String email) {
        return memberRepository.findByEmail(email).orElseThrow();
    }

    public Member findMember(String phone, String birth) {
        return memberRepository.findByPhoneAndBirth(phone, birth);
    }

    public Member findAdministrator() {
        return memberRepository.findByRole(Role.ADMIN);
    }

    public Boolean existEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    public List<Member> findNewUser(LocalDateTime start, LocalDateTime end) {
        return memberRepository.findByRegisterTimeBetween(start, end);
    }

    @Transactional
    public void deleteById(Long id) {
        memberRepository.deleteById(id);
        eventPublisher.publishEvent(new MemberDeletedEvent(id));
    }

    @Transactional
    public void activate(Long id) {
        memberRepository.findById(id).orElseThrow().activate();
        publishMemberChanged(id);
    }

    @Transactional
    public void ban(Long id) {
        memberRepository.findById(id).orElseThrow().ban();
        publishMemberChanged(id);
    }

    @Transactional
    public void updateProfile(Long id, String profile) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 회원입니다."
        ));

        member.changeProfile(profile);
        publishMemberChanged(id);
    }

    @Transactional
    public Member updateLastLoginTime(Long id) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 회원입니다."
        ));

        member.changeLastLoginTime(LocalDateTime.now());
        publishMemberChanged(id);
        return member;
    }

    @Transactional
    public int updatePhoneAndBirth(Long id, String phone, String birth) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 회원입니다."
        ));

        if (memberRepository.existsByPhone(phone))
            return -2;

        String regEx = "(\\d{3})(\\d{3,4})(\\d{4})";
        phone = phone.replaceAll("-", "");
        phone = phone.replaceAll(regEx, "$1-$2-$3");

        member.changePhoneAndBirth(phone, birth);
        publishMemberChanged(id);

        return member.getId().intValue();
    }

    @Transactional
    public int updateRole(Long id, Role role) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 회원입니다."
        ));

        member.changeRole(role);
        publishMemberChanged(id);

        return member.getId().intValue();
    }

    @Transactional
    public long updateVisibility(Long id, CommonVisibility visibility) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 회원입니다."
        ));

        member.changeVisibility(visibility);
        publishMemberChanged(id);
        return member.getId();
    }

    private void publishMemberChanged(Long memberId) {
        eventPublisher.publishEvent(new MemberChangedEvent(memberId));
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException(
                        "이메일이나 비밀번호를 확인해주세요"
                ));
    }
}
