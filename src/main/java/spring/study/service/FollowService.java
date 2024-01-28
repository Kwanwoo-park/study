package spring.study.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
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

    @Transactional
    public Long save(FollowRequestDto followRequestDto) {return followRepository.save(followRequestDto.toEntity()).getId();}

    public HashMap<String, Object> findAll() {
        List<Follow> list = followRepository.findAll(Sort.by("id").descending());

        HashMap<String, Object> map = new HashMap<>();

        map.put("list", list.stream().map(FollowResponseDto::new).collect(Collectors.toList()));

        return map;
    }

    public void deleteFollow(Long following) {
        followRepository.unfollowing(following);
    }
}
