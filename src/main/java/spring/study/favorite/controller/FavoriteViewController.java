package spring.study.favorite.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import spring.study.common.service.SessionService;
import spring.study.favorite.entity.Favorite;
import spring.study.member.entity.Member;
import spring.study.board.service.BoardService;

import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/favorites")
public class FavoriteViewController {
    private final SessionService sessionService;
    private final BoardService boardService;

    @GetMapping("")
    public String getFavorites(Model model, @RequestParam Long id, HttpServletRequest request) throws Exception {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found";

        List<Favorite> favorites = boardService.findById(id).getFavorites();

        model.addAttribute("favorites", favorites);
        model.addAttribute("email", member.getEmail());
        model.addAttribute("following", member.checkFollowing(favorites));

        return "favorite/list";
    }
}
