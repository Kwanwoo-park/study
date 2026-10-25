package spring.study.bookreview.controller;

import spring.study.bookreview.facade.BookReviewViewFacade;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/book-reviews")
public class BookReviewViewController {
    private final BookReviewViewFacade facade;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       Model model,
                       HttpServletRequest request) {
        return facade.list(keyword, page, model, request);
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, HttpServletRequest request) {
        return facade.detail(id, model, request);
    }

    @GetMapping("/write")
    public String write(Model model, HttpServletRequest request) {
        return facade.write(model, request);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, HttpServletRequest request) {
        return facade.edit(id, model, request);
    }


}
