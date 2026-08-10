package spring.study.jwt.dto;

import spring.study.common.entity.CommonVisibility;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

public record MobileMemberResponse(
        Long id,
        String email,
        String name,
        Role role,
        String profile,
        CommonVisibility visibility
) {
    public MobileMemberResponse(Member member) {
        this(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole(),
                member.getProfile(),
                member.getVisibility()
        );
    }
}
