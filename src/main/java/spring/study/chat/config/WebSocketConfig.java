package spring.study.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;
import spring.study.chat.component.ChatChannelAuthorization;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final ChatChannelAuthorization authorization;
    private final List<String> allowedOrigins;

    public WebSocketConfig(ChatChannelAuthorization authorization,
                           @Value("${chat.websocket.allowed-origins:}") List<String> allowedOrigins) {
        this.authorization = authorization;
        this.allowedOrigins = allowedOrigins.stream().filter(origin -> !origin.isBlank()).toList();
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        StompWebSocketEndpointRegistration endpoint = registry.addEndpoint("/ws/chat")
                .addInterceptors(authorization.handshake());
        if (!allowedOrigins.isEmpty()) endpoint.setAllowedOrigins(allowedOrigins.toArray(String[]::new));
        endpoint.withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authorization.inbound());
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(authorization.outbound());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub", "/queue");
        registry.setApplicationDestinationPrefixes("/api");
        registry.setUserDestinationPrefix("/user");
    }
}
