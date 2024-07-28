package spring.study.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.dto.follow.FollowRequestDto;
import spring.study.entity.Follow;
import spring.study.entity.Member;
import spring.study.repository.FollowRepository;

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

    public Follow findFollow(Member follower) {
        return followRepository.findByFollower(follower);
    }

    public void delete(Member follower) {
        followRepository.deleteByFollower(follower);
    }
}
