package spring.study.common.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import spring.study.member.entity.Member;
import spring.study.member.repository.MemberRepository;

/**
 * Compatibility facade for existing controllers. Authentication is now sourced
 * from the JWT-populated SecurityContext; no HTTP session is created or read.
 */
@Service
@RequiredArgsConstructor
public class SessionManager {
    private final MemberRepository memberRepository;

    public Member getLoginMember(HttpServletRequest request) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Member authenticatedMember)) return null;

        Member member = memberRepository.findById(authenticatedMember.getId()).orElse(null);
        if (member == null || member.isAccessBlocked()) {
            SecurityContextHolder.clearContext();
            return null;
        }
        return member;
    }

    public void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
    }
}
