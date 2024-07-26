package spring.study.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.dto.follow.FollowRequestDto;
import spring.study.dto.follow.FollowResponseDto;
import spring.study.entity.Follow;
import spring.study.entity.Member;
import spring.study.repository.FollowRepository;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class FollowService {
    private final FollowRepository followRepository;

    @Transactional
    public Long save(FollowRequestDto followRequestDto) {return followRepository.save(followRequestDto.toEntity()).getId();}

    @Transactional
    public Follow save(Follow follow) {
        return followRepository.save(follow);
    }

    public void delete(Member follower) {
        followRepository.deleteByFollower(follower);
    }
}
