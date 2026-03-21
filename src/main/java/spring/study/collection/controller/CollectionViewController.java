package spring.study.collection.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import spring.study.collection.facade.CollectionViewFacade;
import spring.study.collection.service.CollectionService;
import spring.study.member.entity.Member;

@RequiredArgsConstructor
@Controller
@RequestMapping("/collection")
@Slf4j
public class CollectionViewController {
    private final CollectionViewFacade viewFacade;
    private final CollectionService collectionService;

    @GetMapping("")
    public String collectionMain(Model model,
                                 @RequestParam(name = "cursor", defaultValue = "0") Integer cursor,
                                 @RequestParam(name = "limit", defaultValue = "10") Integer limit,
                                 HttpServletRequest request) {
        Member member = viewFacade.checkIP(request);
        if (member == null) return "/redirect:/board/main";

        model.addAttribute("member", member);
        model.addAttribute("collections", collectionService.getCollections(cursor, limit, member));

        return "collection/collection";
    }
}
