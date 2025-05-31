package spring.study.controller.favorite;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import spring.study.entity.favorite.Favorite;
import spring.study.entity.member.Member;
import spring.study.service.board.BoardService;

import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/favorites")
public class FavoriteViewController {
    private final BoardService boardService;

    @GetMapping("")
    public String getFavorites(Model model, @RequestParam Long id, HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        List<Favorite> favorites = boardService.findById(id).getFavorites();

        model.addAttribute("favorites", favorites);
        model.addAttribute("email", member.getEmail());
        model.addAttribute("following", member.checkFollowing(favorites));

        return "favorite/list";
    }
}
