package spring.study.chat.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import spring.study.admin.service.IpBlockService;
import spring.study.chat.component.ChatChannelAuthorization;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.common.component.IpBlockClientAddress;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebSocketConfigTest {
    private static final String CHAT_ORIGIN = "https://chat.example.test";
    private static final String SOCK_JS_SESSION = "/ws/chat/123/test-session";

    @Test
    void sockJsSendAcceptsTheConfiguredOrigin() throws Exception {
        try (var context = chatContext("chat.websocket.allowed-origins=" + CHAT_ORIGIN)) {
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();

            mvc.perform(post(SOCK_JS_SESSION + "/xhr").header(HttpHeaders.ORIGIN, CHAT_ORIGIN))
                    .andExpect(status().isOk());

            mvc.perform(post(SOCK_JS_SESSION + "/xhr_send")
                            .header(HttpHeaders.ORIGIN, CHAT_ORIGIN)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("[]"))
                    .andExpect(status().isNoContent())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, CHAT_ORIGIN));
        }
    }

    @Test
    void omittedOriginSettingUsesTheSameOriginPolicy() throws Exception {
        try (var context = chatContext()) {
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();

            mvc.perform(get("/ws/chat/info").header(HttpHeaders.ORIGIN, "http://localhost"))
                    .andExpect(status().isOk());
            mvc.perform(get("/ws/chat/info").header(HttpHeaders.ORIGIN, CHAT_ORIGIN))
                    .andExpect(status().isForbidden());
        }
    }

    private AnnotationConfigWebApplicationContext chatContext(String... properties) {
        var context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, properties);
        context.register(WebSocketConfig.class, MvcTestConfiguration.class);
        context.refresh();
        return context;
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc
    static class MvcTestConfiguration {
        @Bean
        ChatChannelAuthorization authorization() {
            return new ChatChannelAuthorization(mock(ChatRoomMemberService.class), new IpBlockClientAddress(List.of()),
                    mock(IpBlockService.class));
        }
    }
}
