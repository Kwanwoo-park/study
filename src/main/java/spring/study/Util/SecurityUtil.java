package spring.study.Util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
public class SecurityUtil {
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null)
            throw new RuntimeException("No authentication information");

        log.info("Name: {}", authentication.getName());
        log.info("Detail: {}", authentication.getDetails());
        log.info("Authorities: {}", authentication.getAuthorities());
        log.info("Credentials: {}", authentication.getCredentials());
        log.info("Principal: {}", authentication.getPrincipal());

        return authentication.getName();
    }

}
