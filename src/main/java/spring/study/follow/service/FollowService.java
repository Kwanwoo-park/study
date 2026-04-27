package spring.study.follow.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import spring.study.follow.dto.FollowRequestDto;
import spring.study.follow.entity.Follow;
import spring.study.member.entity.Member;
import spring.study.follow.repository.FollowRepository;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class FollowService {
    private final FollowRepository followRepository;

    @Transactional
    public Long save(FollowRequestDto followRequestDto) {
        return followRepository.save(followRequestDto.toEntity()).getId();
    }

    @Transactional
    public Follow save(Follow follow) {
        return followRepository.save(follow);
    }

    @Transactional
    public Follow save(Member member, Member search_member) {
        Follow follow = Follow.builder()
                .follower(member)
                .following(search_member)
                .build();

        member.addFollower(follow);
        search_member.addFollowing(follow);

        return followRepository.save(follow);
    }

    public Follow findFollow(Member follower, Member following) {
        return followRepository.findByFollowerAndFollowing(follower, following);
    }

    public Boolean existFollow(Member follower, Member following) {
        return followRepository.existsByFollowerAndFollowing(follower, following);
    }

    public List<Member> getMemberList(Member member) {
        List<Follow> list = followRepository.findByFollower(member);
        List<Member> memberList = new ArrayList<>();

        memberList.add(member);

        for (Follow follow : list) {
            memberList.add(follow.getFollowing());
        }

        return memberList;
    }

    public List<Follow> getFollowers(Member member, int cursor, int limit) {
        return followRepository.findByFollowing(
                member,
                PageRequest.of(cursor, limit, Sort.by("id").descending())
        );
    }

    public List<Follow> getFollowing(Member member, int cursor, int limit) {
        return followRepository.findByFollower(
                member,
                PageRequest.of(cursor, limit, Sort.by("id").descending())
        );
    }

    public long countFollowers(Member member) {
        return followRepository.countByFollowing(member);
    }

    public long countFollowing(Member member) {
        return followRepository.countByFollower(member);
    }

    @Transactional
    public void delete(Member follower, Member following) {
        followRepository.deleteByFollowerAndFollowing(follower, following);
    }

    @Transactional
    public void delete(Follow follow, Member member) {
        member.removeFollower(follow);

        followRepository.deleteById(follow.getId());
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
