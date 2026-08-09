package spring.study.regression;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigRegressionTest {
    private static final Path SECURITY_CONFIG = Path.of(
            "src/main/java/spring/study/member/config/SecurityConfig.java"
    );

    @Test
    void adminAuthorizationShouldNotBeShadowedByCatchAllPermitRule() throws IOException {
        String config = Files.readString(SECURITY_CONFIG);

        assertTrue(config.contains(".requestMatchers(\"/admin/**\").hasRole(\"ADMIN\")"),
                "admin pages should require the admin role");
        assertTrue(config.contains(".requestMatchers(\"/admin/access-denied\").permitAll()"),
                "the access guidance page must remain reachable after authorization fails");
        assertTrue(config.contains(".accessDeniedPage(\"/admin/access-denied\")"),
                "forbidden admin page requests should display the access guidance page");
        assertTrue(config.contains(".anyRequest().permitAll()"),
                "non-admin routes should remain publicly reachable while controllers enforce JWT authentication");
        assertTrue(config.contains("SessionCreationPolicy.STATELESS"),
                "JWT authentication must not create an HTTP session");
        assertTrue(config.contains("addFilterBefore(jwtAuthenticationFilter"),
                "the JWT authentication filter must run before username/password authentication");
        assertFalse(config.contains(".requestMatchers(\"/**\""),
                "a catch-all request matcher must not shadow the admin authorization rule");
        assertTrue(config.indexOf(".requestMatchers(\"/admin/**\").hasRole(\"ADMIN\")")
                        < config.indexOf(".anyRequest().permitAll()"),
                "the admin rule should be evaluated before the fallback rule");
        assertTrue(config.indexOf(".requestMatchers(\"/admin/access-denied\").permitAll()")
                        < config.indexOf(".requestMatchers(\"/admin/**\").hasRole(\"ADMIN\")"),
                "the guidance page exception must be declared before the admin wildcard rule");
    }
}
