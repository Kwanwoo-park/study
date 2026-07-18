package spring.study.member.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;

@Component
public class JwtCookieService {
    public static final String ACCESS_COOKIE = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";
    public static final String OAUTH2_REQUEST_COOKIE = "oauth2_request";

    @Value("${security.jwt.secure-cookie}")
    private boolean secureCookie;

    public String read(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    public void writeAccessToken(HttpServletResponse response, String value, Duration maxAge) {
        write(response, ACCESS_COOKIE, value, maxAge, true);
    }

    public void writeRefreshToken(HttpServletResponse response, String value, Duration maxAge) {
        write(response, REFRESH_COOKIE, value, maxAge, true);
    }

    public void writeOAuth2Request(HttpServletResponse response, String value) {
        write(response, OAUTH2_REQUEST_COOKIE, value, Duration.ofMinutes(5), true);
    }

    public void clearAuthenticationCookies(HttpServletResponse response) {
        clear(response, ACCESS_COOKIE);
        clear(response, REFRESH_COOKIE);
    }

    public void clearOAuth2Request(HttpServletResponse response) {
        clear(response, OAUTH2_REQUEST_COOKIE);
    }

    private void clear(HttpServletResponse response, String name) {
        write(response, name, "", Duration.ZERO, true);
    }

    private void write(HttpServletResponse response, String name, String value, Duration maxAge, boolean httpOnly) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
