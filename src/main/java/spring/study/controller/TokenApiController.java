package spring.study.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.study.component.JwtTokenProvider;

import java.io.IOException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/jwt")
@Slf4j
public class TokenApiController {
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/auth")
    public ResponseEntity<Authentication> auth(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String accessToken = request.getHeader("Authorization");
        Authentication authentication = jwtTokenProvider.getAuthentication(accessToken.substring(7));

        System.out.println(authentication);

        if (authentication.isAuthenticated()) {
            response.sendRedirect("/board/list");
            return ResponseEntity.ok(authentication);
        }
        else
            return ResponseEntity.status(501).body(null);
    }
}
