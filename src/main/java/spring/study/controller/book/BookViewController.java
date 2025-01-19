package spring.study.controller.book;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.book.BookRequestDto;
import spring.study.entity.member.Member;
import spring.study.entity.member.Role;
import spring.study.service.book.BookBorrowService;
import spring.study.service.book.BookService;

import java.util.HashMap;

@RequiredArgsConstructor
@Controller
@RequestMapping("/book")
public class BookViewController {
    private final BookService bookService;
    private final BookBorrowService bookBorrowService;

    @GetMapping("/list")
    public String list(Model model,
                       @RequestParam(required = false, defaultValue = "0") Integer page,
                       @RequestParam(required = false, defaultValue = "5") Integer size,
                       HttpSession session) {
        if (session == null) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        HashMap<String, Object> book = (HashMap<String, Object>) session.getAttribute("book");
        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        if (member.getRole() != Role.ADMIN) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        if (book != null)
            session.removeAttribute("book");

        model.addAttribute("book", bookService.findAll(page, size));


        return "/book/list";
    }

    @GetMapping("/find")
    public String bookFind(BookRequestDto bookRequestDto, Model model, HttpSession session,
                           @RequestParam(required = false, defaultValue = "0") Integer page,
                           @RequestParam(required = false, defaultValue = "5") Integer size) {
        if (session == null) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        if (member.getRole() != Role.ADMIN) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        model.addAttribute("book", bookService.findBook(bookRequestDto.getTitle(), page, size));
        model.addAttribute("title", bookRequestDto.getTitle());

        return "/book/bookFind";
    }

    @GetMapping("/borrowList")
    public String bookBorrowList(BookRequestDto bookRequestDto, Model model, HttpSession session,
                           @RequestParam(required = false, defaultValue = "0") Integer page,
                           @RequestParam(required = false, defaultValue = "5") Integer size) {
        if (session == null) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        if (member.getRole() != Role.ADMIN) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        model.addAttribute("book", bookService.findBorrow(bookRequestDto.getBorw(), page, size));
        model.addAttribute("borrow", bookRequestDto.getBorw());

        return "/book/borrowList";
    }

    @GetMapping("/detail")
    public String detail(@RequestParam() String title, Model model, HttpSession session) {
        if (session == null) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        if (member.getRole() != Role.ADMIN) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        model.addAttribute("book", bookService.findBookByTitle(title));
        //model.addAttribute("name", bookBorrowService.findTitle(title));

        return "/book/detail";
    }
}
