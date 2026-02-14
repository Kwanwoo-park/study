package spring.study.favorite.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.common.service.SessionService;
import spring.study.favorite.facade.FavoriteFacade;
import spring.study.member.entity.Member;

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
