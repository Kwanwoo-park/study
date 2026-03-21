package spring.study.collection.facade;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.study.common.service.IPEntityService;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionViewFacade {
    private final IPEntityService ipEntityService;
    private final MemberService memberService;

    public Member checkIP(HttpServletRequest request) {
        String ip = ipEntityService.getIp(request);

        if (ipEntityService.exist(ip) == false)
            return null;


        return memberService.findById(ipEntityService.findByIp(ip).getMemberId());
    }
}
