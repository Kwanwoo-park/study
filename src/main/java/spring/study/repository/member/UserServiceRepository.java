package spring.study.repository.member;

import org.springframework.stereotype.Repository;
import spring.study.dto.member.MemberRequestDto;
import spring.study.dto.member.MemberResponseDto;

@Repository
public interface UserServiceRepository {
    MemberResponseDto createUser(MemberRequestDto memberRequestDto);
}
