package spring.study.service.member;

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
import spring.study.dto.member.MemberRequestDto;
import spring.study.dto.member.MemberResponseDto;
import spring.study.entity.member.Member;
import spring.study.entity.member.Role;
import spring.study.repository.member.MemberRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
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

    public HashMap<String, Object> findName(String name) {
        HashMap<String, Object> member = new HashMap<>();

        List<Member> list = memberRepository.findByName(name);

        member.put("list", list.stream().map(MemberResponseDto::new).collect(Collectors.toList()));

        return member;
    }

    public Member findMember(String email) {
        return memberRepository.findByEmail(email).orElseThrow();
    }

    public Member findMember(String phone, String birth) {
        return memberRepository.findByPhoneAndBirth(phone, birth);
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
    public void updatePhoneAndBirth(Long id, String phone, String birth) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 회원입니다."
        ));

        member.changePhoneAndBirth(phone, birth);
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
