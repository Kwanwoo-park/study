package spring.study.diary.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.JwtManager;
import spring.study.diary.dto.DiaryResponseDto;
import spring.study.diary.facade.DiaryFacade;
import spring.study.member.entity.Member;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiaryApiControllerTest {
    private final DiaryFacade facade = mock(DiaryFacade.class);
    private final JwtManager jwtManager = mock(JwtManager.class);
    private final CommonFacade common = mock(CommonFacade.class);
    private final DiaryApiController controller = new DiaryApiController(facade, jwtManager, common);
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Test
    void detailReturnsOwnedDiaryThroughExistingOwnershipCheck() {
        Member member = Member.builder().id(7L).build();
        DiaryResponseDto diary = mock(DiaryResponseDto.class);
        when(jwtManager.getLoginMember(request)).thenReturn(member);
        when(facade.findById(12L, member)).thenReturn(diary);
        assertThat(controller.detail(12L, request).getBody()).isSameAs(diary);
        verify(facade).findById(12L, member);
    }

    @Test
    void detailRejectsAnonymousRequestsWithoutLoadingDiary() {
        doReturn(ResponseEntity.status(401).build()).when(common).unauthorized();
        assertThat(controller.detail(12L, request).getStatusCode().value()).isEqualTo(401);
        verifyNoInteractions(facade);
    }

    @Test
    void anotherMembersDiaryIsNotExposed() {
        Member member = Member.builder().id(7L).build();
        when(jwtManager.getLoginMember(request)).thenReturn(member);
        when(facade.findById(12L, member)).thenThrow(new AccessDeniedException("Forbidden"));
        assertThatThrownBy(() -> controller.detail(12L, request)).isInstanceOf(AccessDeniedException.class);
    }
}
