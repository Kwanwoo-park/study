package spring.study.bookreview.facade;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.ui.Model;
import spring.study.bookreview.dto.BookReviewResponseDto;
import spring.study.bookreview.service.BookReviewService;
import spring.study.common.exception.ResourceNotFoundException;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BookReviewViewFacade {
    private static final int PAGE_SIZE = 9;
    private final BookReviewService bookReviewService;
    private final JwtManager jwtManager;

    public String list(String keyword,
                       int page,
                       Model model,
                       HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) {
            return "redirect:/member/login?error=true&exception=Not Found&url=/book-reviews";
        }

        Page<BookReviewResponseDto> reviews = bookReviewService.findAll(keyword, page, PAGE_SIZE);
        addCommonModel(model, member);
        model.addAttribute("reviews", reviews);
        model.addAttribute("keyword", keyword == null ? "" : keyword.trim());
        return "bookreview/list";
    }

    public String detail(Long id, Model model, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) {
            return "redirect:/member/login?error=true&exception=Not Found&url=/book-reviews/" + id;
        }

        try {
            addCommonModel(model, member);
            model.addAttribute("review", bookReviewService.findById(id));
            return "bookreview/detail";
        } catch (ResourceNotFoundException exception) {
            return "error/404";
        }
    }

    public String write(Model model, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) {
            return "redirect:/member/login?error=true&exception=Not Found&url=/book-reviews/write";
        }
        if (member.getRole() != Role.ADMIN) return "error/404";

        addCommonModel(model, member);
        return "bookreview/form";
    }

    public String edit(Long id, Model model, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) {
            return "redirect:/member/login?error=true&exception=Not Found&url=/book-reviews/" + id + "/edit";
        }
        if (member.getRole() != Role.ADMIN) return "error/404";

        try {
            addCommonModel(model, member);
            model.addAttribute("review", bookReviewService.findById(id));
            return "bookreview/form";
        } catch (ResourceNotFoundException exception) {
            return "error/404";
        }
    }

    private void addCommonModel(Model model, Member member) {
        model.addAttribute("email", member.getEmail());
        model.addAttribute("profile", member.getProfile());
        model.addAttribute("isAdmin", member.getRole() == Role.ADMIN);
    }
}
