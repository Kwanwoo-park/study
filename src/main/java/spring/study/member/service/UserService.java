package spring.study.member.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.dto.MemberResponseDto;
import spring.study.member.entity.Member;
import spring.study.member.repository.MemberRepository;
import spring.study.member.repository.UserServiceRepository;
import spring.study.member.entity.Role;

@Service
@RequiredArgsConstructor
public class UserService implements UserServiceRepository {
    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public String replacePhoneNumber(String phone) {
        phone = phone.replaceAll("-", "");
        String regEx = "(\\d{3})(\\d{3,4})(\\d{4})";

        return phone.replaceAll(regEx, "$1-$2-$3");
    }

    @Override
    public MemberResponseDto createUser(MemberRequestDto memberRequestDto) {
        if (memberRepository.existsByEmail(memberRequestDto.getEmail()))
            return null;

        if (memberRepository.existsByPhone(memberRequestDto.getPhone()))
            return null;

        Member member = memberRepository.save(Member.builder()
                        .email(memberRequestDto.getEmail())
                        .pwd(bCryptPasswordEncoder.encode(memberRequestDto.getPassword()))
                        .name(memberRequestDto.getName())
                        .phone(replacePhoneNumber(memberRequestDto.getPhone()))
                        .birth(memberRequestDto.getBirth())
                        .role(Role.USER)
                        .profile("KakaoTalk_Photo_2023-04-14-21-36-15.jpeg")
                        .build());

        return new MemberResponseDto(member);
    }

    @Transactional
    public long updatePwd(Long id, String pwd) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 회원입니다."
        ));

        member.changePwd(bCryptPasswordEncoder.encode(pwd));

        return member.getId();
    }
}
