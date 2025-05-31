package spring.study.member.repository;

import org.springframework.stereotype.Repository;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.dto.MemberResponseDto;

@Repository
public interface UserServiceRepository {
    MemberResponseDto createUser(MemberRequestDto memberRequestDto);
}
