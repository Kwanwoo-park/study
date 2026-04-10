package spring.study.collection.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import spring.study.common.service.SessionManager;
import spring.study.member.entity.Member;

@RequiredArgsConstructor
@Controller
@RequestMapping("/collection")
@Slf4j
public class CollectionViewController {
    private final SessionManager sessionManager;

    @GetMapping("")
    public String collectionMain(Model model, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found";

        model.addAttribute("member", member);

        log.info("size = {}", member.getCollections().size());

        return "collection/collection";
    }
}
