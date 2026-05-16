package spring.study.favorite.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.SessionManager;
import spring.study.favorite.facade.FavoriteFacade;
import spring.study.member.entity.Member;


@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/favorite")
public class FavoriteApiController {
    private final SessionManager sessionManager;
    private final CommonFacade commonFacade;
    private final FavoriteFacade favoriteFacade;

    @GetMapping("/list")
    public ResponseEntity<?> getFavoriteList(@RequestParam Long id,
                                             @RequestParam(defaultValue = "0", name = "cursor") int cursor,
                                             @RequestParam(defaultValue = "10", name = "limit") int limit,
                                             HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return favoriteFacade.getList(id, member, cursor, limit);
    }

    @PostMapping("/like")
    public ResponseEntity<?> favoriteAction(@RequestParam Long id, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return favoriteFacade.like(id, member);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> favoriteDelete(@RequestParam Long id, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return favoriteFacade.unlike(id, member);
    }
}
