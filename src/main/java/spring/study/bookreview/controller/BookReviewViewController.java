package spring.study.bookreview.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import spring.study.bookreview.dto.BookReviewResponseDto;
import spring.study.bookreview.service.BookReviewService;
import spring.study.common.exception.ResourceNotFoundException;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

@Controller
@RequiredArgsConstructor
@RequestMapping("/book-reviews")
public class BookReviewViewController {
    private static final int PAGE_SIZE = 9;

    private final BookReviewService bookReviewService;
    private final JwtManager jwtManager;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "0") int page,
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

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, HttpServletRequest request) {
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

    @GetMapping("/write")
    public String write(Model model, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) {
            return "redirect:/member/login?error=true&exception=Not Found&url=/book-reviews/write";
        }
        if (member.getRole() != Role.ADMIN) return "error/404";

        addCommonModel(model, member);
        return "bookreview/form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, HttpServletRequest request) {
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
