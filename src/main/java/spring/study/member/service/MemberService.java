package spring.study.member.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.dto.MemberResponseDto;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
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
        return memberRepository.findByName(name).stream().map(MemberResponseDto::new).toList();
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

    public void deleteById(Long id) { memberRepository.deleteById(id); }

    @Transactional
    public void updateProfile(Long id, String profile) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 회원입니다."
        ));

        member.changeProfile(profile);
    }

    @Transactional
    public void updateLastLoginTime(Long id) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 회원입니다."
        ));

        member.changeLastLoginTime(LocalDateTime.now());
    }

    @Transactional
    public int updatePhone(Long id, String phone) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 회원입니다."
        ));

        String regEx = "(\\d{3})(\\d{3,4})(\\d{4})";
        phone = phone.replaceAll(regEx, "$1-$2-$3");

        member.changePhone(phone);

        return member.getId().intValue();
    }

    @Transactional
    public int updatePhoneAndBirth(Long id, String phone, String birth) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 회원입니다."
        ));

        member.changePhoneAndBirth(phone, birth);

        return member.getId().intValue();
    }

    @Transactional
    public int updateRole(Long id, Role role) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 회원입니다."
        ));

        member.changeRole(role);

        return member.getId().intValue();
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException(
                        "이메일이나 비밀번호를 확인해주세요"
                ));
    }
}
