package spring.study.collection.facade;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import spring.study.collection.service.CollectionService;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CollectionViewFacade {
    private final JwtManager jwtManager;
    private final CollectionService collectionService;

    public String collectionMain(Model model, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found&url=/collection";

        model.addAttribute("member", member);
        model.addAttribute("count", collectionService.countByMember(member));

        return "collection/collection";
    }
}
