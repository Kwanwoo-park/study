package spring.study.repository.member;

import spring.study.dto.member.MemberRequestDto;
import spring.study.dto.member.MemberResponseDto;

public interface UserServiceRepository {
    MemberResponseDto createUser(MemberRequestDto memberRequestDto);
}
