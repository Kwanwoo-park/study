package spring.study.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.dto.follow.FollowRequestDto;
import spring.study.dto.follow.FollowResponseDto;
import spring.study.entity.follow.Follow;
import spring.study.entity.follow.FollowRepository;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class FollowService {
    private final FollowRepository followRepository;

    private HashMap<String, Object> map = new HashMap<>();
    private List<Follow> list;

    @Transactional
    public Long save(FollowRequestDto followRequestDto) {return followRepository.save(followRequestDto.toEntity()).getId();}

    public HashMap<String, Object> findAll() {
        list = followRepository.findAll();

        map.put("list", list.stream().map(FollowResponseDto::new).collect(Collectors.toList()));

        return map;
    }

    public Long countFollowing(Long following) { return followRepository.countByFollowing(following); }

    public HashMap<String, Object> findFollowing(Long following) {
        list = followRepository.findByFollowing(following);

        map.put("list", list.stream().map(FollowResponseDto::new).toList());

        return map;
    }

    public Long countFollower(Long follower) { return followRepository.countByFollower(follower); }

    public HashMap<String, Object> findFollower(Long follower) {
        list = followRepository.findByFollower(follower);

        map.put("list", list.stream().map(FollowResponseDto::new).toList());

        return map;
    }

    public void deleteFollow(Long following) {
        followRepository.unfollowing(following);
    }
}
