package spring.study.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import spring.study.dto.member.MemberRequestDto;
import spring.study.dto.member.MemberResponseDto;
import spring.study.entity.member.Member;
import spring.study.entity.member.MemberRepository;
import spring.study.entity.member.UserServiceRepository;
import spring.study.entity.role.Role;

@Service
@RequiredArgsConstructor
public class UserService implements UserServiceRepository {
    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    public MemberResponseDto createUser(MemberRequestDto memberRequestDto) {
        if (memberRepository.findByEmail(memberRequestDto.getEmail()) != null)
            return null;

        Member member = memberRepository.save(Member.builder()
                        .email(memberRequestDto.getEmail())
                        .pwd(bCryptPasswordEncoder.encode(memberRequestDto.getPassword()))
                        .name(memberRequestDto.getName())
                        .role(Role.USER).build());

        return new MemberResponseDto(member);
    }
}
