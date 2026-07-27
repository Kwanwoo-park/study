package spring.study.favorite.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;

@RequiredArgsConstructor
@Controller
@RequestMapping("/favorites")
public class FavoriteViewController {
    private final JwtManager jwtManager;

    @GetMapping("")
    public String getFavorites(Model model, @RequestParam Long id, HttpServletRequest request) throws Exception {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found&url=/favorite?id=" + id;

        model.addAttribute("boardId", id);
        model.addAttribute("email", member.getEmail());

        return "favorite/list";
    }
}
