package spring.study.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.dto.follow.FollowRequestDto;
import spring.study.dto.follow.FollowResponseDto;
import spring.study.entity.Follow;
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

    public HashMap<String, Object> findAll() {
        HashMap<String, Object> map = new HashMap<>();

        List<Follow> list = followRepository.findAll();

        map.put("list", list.stream().map(FollowResponseDto::new).collect(Collectors.toList()));

        return map;
    }

    public Long countFollowing(Long following) { return followRepository.countByFollowing(following); }

    public List<Follow> findFollowing(Long following) {
        return followRepository.findByFollowing(following);
    }

    public Long countFollower(Long follower) { return followRepository.countByFollower(follower); }

    public List<Follow> findFollower(Long follower) {
        return followRepository.findByFollower(follower);
    }
}
