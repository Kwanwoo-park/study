package spring.study.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.data.repository.query.Param;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.dto.member.MemberRequestDto;
import spring.study.entity.member.Member;
import spring.study.entity.member.MemberRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class MemberService implements UserDetailsService {
    private final MemberRepository memberRepository;

    @Transactional
    public Long save(MemberRequestDto memberSaveDto) {
        return memberRepository.save(memberSaveDto.toEntity()).getId();
    }

    public Optional<Member> findById(Long id) {
        return memberRepository.findById(id);
    }

    public int updateMemberLastLogin(@Param("email") String email,
                                     @Param("lastLoginTime") LocalDateTime lastLoginTime) {
        return memberRepository.updateMemberLastLogin(email, lastLoginTime);
    }

    public int updateMemberPassword(@Param("email") String email, @Param("password") String password) {
        return memberRepository.updateMemberPassword(email, new BCryptPasswordEncoder().encode(password));
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return Optional
                .ofNullable(memberRepository.findByEmail(email))
                .orElseThrow(() -> new BadCredentialsException(
                        "이메일이나 비밀번호를 확인해주세요"
                ));
    }
}
