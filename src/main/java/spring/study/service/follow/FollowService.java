package spring.study.service.follow;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.dto.follow.FollowRequestDto;
import spring.study.entity.follow.Follow;
import spring.study.entity.member.Member;
import spring.study.repository.follow.FollowRepository;

import java.util.List;

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

    public Follow findFollow(Member follower, Member following) {
        return followRepository.findByFollowerAndFollowing(follower, following);
    }

    public Boolean existFollow(Member follower, Member following) {
        return followRepository.existsByFollowerAndFollowing(follower, following);
    }

    public void delete(Member follower, Member following) {
        followRepository.deleteByFollowerAndFollowing(follower, following);
    }

    public void deleteByFollower(Member follower) {
        followRepository.deleteByFollower(follower);
    }

    public void deleteByFollowing(Member following) {
        followRepository.deleteByFollowing(following);
    }

    public List<Follow> findAll() {
        return followRepository.findAll();
    }
}
