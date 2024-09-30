package spring.study.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.entity.RefreshToken;
import spring.study.repository.RefreshTokenRepository;

@RequiredArgsConstructor
@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshToken findByToken(String refreshToken) {
        return refreshTokenRepository.findByRefreshToken(refreshToken).orElseThrow(() ->
                new RuntimeException("Refresh Token not found")
        );
    }
}
