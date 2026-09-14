package spring.study.notification.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.EmitterService;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;
import spring.study.notification.facade.NotificationFacade;
import spring.study.notification.service.NotificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class NotificationApiControllerTest {
    @Test
    void streamUsesAuthenticatedMemberIdAndReturnsTheSameEmitter() {
        EmitterService emitterService = mock(EmitterService.class);
        JwtManager jwtManager = mock(JwtManager.class);
        NotificationFacade facade = new NotificationFacade(mock(NotificationService.class), emitterService);
        NotificationApiController controller = new NotificationApiController(jwtManager, new CommonFacade(), facade);
        MockHttpServletRequest request = new MockHttpServletRequest();
        SseEmitter emitter = new SseEmitter();

        assertThat(controller.streamNotification(request)).isNull();
        verifyNoInteractions(emitterService);

        when(jwtManager.getLoginMember(request)).thenReturn(Member.builder().id(7L).build());
        when(emitterService.addEmitter("7")).thenReturn(emitter);

        assertThat(controller.streamNotification(request)).isSameAs(emitter);
        verify(emitterService).addEmitter("7");
    }
}
