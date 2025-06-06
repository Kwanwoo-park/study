package spring.study.oauth.service;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import spring.study.oauth.dto.OAuthAttributes;
import spring.study.member.entity.Member;
import spring.study.member.repository.MemberRepository;

import java.time.LocalDateTime;
import java.util.Collections;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final MemberRepository memberRepository;
    private final HttpSession session;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        Member member = saveOrUpate(attributes);
        session.setAttribute("member", member);

        return new DefaultOAuth2User(Collections.singleton(new SimpleGrantedAuthority(member.getRole().getValue())), attributes.getAttributes(), attributes.getNameAttributeKey());
    }

    private Member saveOrUpate(OAuthAttributes attributes) {
        if (memberRepository.existsByEmail(attributes.getEmail())) {
            Member member = memberRepository.findByEmail(attributes.getEmail()).orElseThrow(() -> new BadCredentialsException(
                    "존재하지 않는 회원입니다."
            ));

            member.changeLastLoginTime(LocalDateTime.now());
            return member;
        }
        else
            return memberRepository.save(attributes.toEntity());
    }
}