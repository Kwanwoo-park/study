package spring.study.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import spring.study.dto.member.MemberRequestDto;
import spring.study.dto.member.MemberResponseDto;
import spring.study.entity.Member;
import spring.study.repository.MemberRepository;
import spring.study.repository.UserServiceRepository;
import spring.study.entity.Role;

@Service
@RequiredArgsConstructor
public class UserService implements UserServiceRepository {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public MemberResponseDto createUser(MemberRequestDto memberRequestDto) {
        if (memberRepository.findByEmail(memberRequestDto.getEmail()).isPresent())
            return null;

        String regEx = "(\\d{3})(\\d{3,4})(\\d{4})";
        String phone = memberRequestDto.getPhone().replaceAll(regEx, "$1-$2-$3");

        Member member = memberRepository.save(Member.builder()
                        .email(memberRequestDto.getEmail())
                        .pwd(passwordEncoder.encode(memberRequestDto.getPassword()))
                        .name(memberRequestDto.getName())
                        .phone(phone)
                        .birth(memberRequestDto.getBirth())
                        .role(Role.USER)
                        .profile("KakaoTalk_Photo_2023-04-14-21-36-15.jpeg")
                        .build());

        return new MemberResponseDto(member);
    }

    @Transactional
    public int updatePwd(Long id, String pwd) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 회원입니다."
        ));

        member.changePwd(passwordEncoder.encode(pwd));

        return member.getId().intValue();
    }

    private UserDetails createUserDetails(Member member) {
        return User.builder()
                .username(member.getUsername())
                .password(passwordEncoder.encode(member.getPassword()))
                .roles(member.getRole().toString())
                .build();
    }
}
