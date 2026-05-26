package spring.study.common.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Locale;

@ControllerAdvice
public class MobileViewModelAdvice {
    private static final String[] MOBILE_UA_TOKENS = {
            "mobile", "android", "iphone", "ipad", "ipod",
            "blackberry", "windows phone", "opera mini", "iemobile"
    };

    @ModelAttribute("isMobile")
    public boolean isMobile(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String userAgent = session == null ? null : (String) session.getAttribute("UA");

        if (userAgent == null || userAgent.isBlank()) {
            userAgent = request.getHeader("User-Agent");
        }

        if (userAgent == null) {
            return false;
        }

        String normalizedUserAgent = userAgent.toLowerCase(Locale.ROOT);
        for (String token : MOBILE_UA_TOKENS) {
            if (normalizedUserAgent.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
