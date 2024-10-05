package spring.study.config;

import lombok.RequiredArgsConstructor;

import java.security.Principal;

@RequiredArgsConstructor
public class JwtPrincipal implements Principal {
    private final String username;

    @Override
    public String getName() {
        return username;
    }

}
