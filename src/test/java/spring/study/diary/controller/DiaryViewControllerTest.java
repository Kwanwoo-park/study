package spring.study.diary.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import spring.study.common.service.JwtManager;
import spring.study.diary.dto.DiaryResponseDto;
import spring.study.diary.facade.DiaryFacade;
import spring.study.member.entity.Member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DiaryViewControllerTest {
    private final DiaryFacade facade = mock(DiaryFacade.class);
    private final JwtManager jwtManager = mock(JwtManager.class);
    private final DiaryViewController controller = new DiaryViewController(facade, jwtManager);
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Test
    void editViewReceivesTheDiaryDtoRatherThanAnHttpResponse() {
        Member member = Member.builder().id(7L).build();
        DiaryResponseDto diary = new DiaryResponseDto();
        ExtendedModelMap model = new ExtendedModelMap();
        when(jwtManager.getLoginMember(request)).thenReturn(member);
        when(facade.findDetail(12L, member)).thenReturn(diary);

        assertThat(controller.write(12L, model, request)).isEqualTo("diary/write");
        assertThat(model.get("diary")).isSameAs(diary);
        verify(facade, never()).findById(anyLong(), any());
    }

    @Test
    void anonymousEditDoesNotLoadTheDiary() {
        assertThat(controller.write(12L, new ExtendedModelMap(), request)).startsWith("redirect:/member/login");
        verifyNoInteractions(facade);
    }
}
