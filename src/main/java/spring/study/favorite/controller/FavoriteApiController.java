package spring.study.favorite.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.board.entity.Board;
import spring.study.common.service.SessionService;
import spring.study.favorite.entity.Favorite;
import spring.study.favorite.facade.FavoriteFacade;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Group;
import spring.study.board.service.BoardService;
import spring.study.favorite.service.FavoriteService;
import spring.study.notification.service.NotificationService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/favorite")
public class FavoriteApiController {
    private final SessionService sessionService;
    private final FavoriteFacade favoriteFacade;

    @PostMapping("/like")
    public ResponseEntity<?> favoriteAction(@RequestParam Long id, HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return favoriteFacade.like(id, member, request);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> favoriteDelete(@RequestParam Long id, HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return favoriteFacade.unlike(id, member, request);
    }
}
