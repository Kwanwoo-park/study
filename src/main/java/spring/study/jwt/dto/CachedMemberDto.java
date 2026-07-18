package spring.study.jwt.dto;

import spring.study.member.entity.AccountStatus;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.time.LocalDateTime;

/**
 * Redis representation of the authenticated member. Only scalar values needed
 * by the security principal are cached; credentials and JPA associations are
 * deliberately excluded.
 */
public record CachedMemberDto(
        Long id,
        String email,
        String name,
        Role role,
        AccountStatus accountStatus,
        LocalDateTime suspendedUntil,
        int warningCount,
        String phone,
        String birth,
        String profile,
        LocalDateTime lastLoginTime
) {
    public static CachedMemberDto from(Member member) {
        return new CachedMemberDto(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole(),
                member.getAccountStatus(),
                member.getSuspendedUntil(),
                member.getWarningCount(),
                member.getPhone(),
                member.getBirth(),
                member.getProfile(),
                member.getLastLoginTime()
        );
    }

    public Member toMember() {
        return Member.builder()
                .id(id)
                .email(email)
                .name(name)
                .role(role)
                .accountStatus(accountStatus)
                .suspendedUntil(suspendedUntil)
                .warningCount(warningCount)
                .phone(phone)
                .birth(birth)
                .profile(profile)
                .lastLoginTime(lastLoginTime)
                .build();
    }
}
