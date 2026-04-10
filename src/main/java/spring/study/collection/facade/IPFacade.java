package spring.study.collection.facade;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.study.common.entity.IPEntity;
import spring.study.common.service.IPEntityService;
import spring.study.common.service.SessionManager;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;

@Service
@RequiredArgsConstructor
@Slf4j
public class IPFacade {
    private final IPEntityService ipEntityService;
    private final SessionManager sessionManager;
    private final MemberService memberService;

    public Member checkIP(HttpServletRequest request) {
        HttpSession session = sessionManager.getSession(request);

        if (session == null) return null;

        String ip = session.getAttribute("IP").toString();
        Member member = (Member) session.getAttribute("member");
        Long id = member.getId();

        if (ipEntityService.exist(ip, id) == false)
            return null;

        return member;
    }

    public IPEntity saveIp(HttpServletRequest request) {
        HttpSession session = sessionManager.getSession(request);

        if (session == null) return null;

        String ip = session.getAttribute("IP").toString();
        Member member = (Member) session.getAttribute("member");
        Long id = member.getId();

        return ipEntityService.save(IPEntity.builder()
                .ip(ip)
                .memberId(id)
                .build());
    }
}
