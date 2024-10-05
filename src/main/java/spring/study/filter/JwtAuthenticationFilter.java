package spring.study.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import spring.study.component.JwtTokenProvider;
import spring.study.config.JwtPrincipal;
import spring.study.entity.Member;
import spring.study.service.MemberService;

import java.io.IOException;
import java.util.Arrays;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberService memberService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getEmail(token);

            Member member = (Member) memberService.loadUserByUsername(email);

            if (member != null) {
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(new JwtPrincipal(member.getUsername()), null, Arrays.asList(new SimpleGrantedAuthority(member.getRole().getValue())));

                SecurityContext context = SecurityContextHolder.getContext();
                context.setAuthentication(usernamePasswordAuthenticationToken);

                System.out.println(SecurityContextHolder.getContext().getAuthentication());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = null;
        for (Cookie c : request.getCookies()) {
            if (c.getName().equals("accessToken"))
                bearerToken = c.getValue();
        }

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer=")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
