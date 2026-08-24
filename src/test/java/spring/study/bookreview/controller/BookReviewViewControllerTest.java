package spring.study.bookreview.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.ui.ExtendedModelMap;
import spring.study.bookreview.dto.BookReviewResponseDto;
import spring.study.bookreview.service.BookReviewService;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BookReviewViewControllerTest {
    private final BookReviewService bookReviewService = mock(BookReviewService.class);
    private final JwtManager jwtManager = mock(JwtManager.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private BookReviewViewController controller;

    @BeforeEach
    void setUp() {
        controller = new BookReviewViewController(bookReviewService, jwtManager);
    }

    @Test
    void regularMemberCanOpenListAndDetailPages() {
        Member member = member(Role.USER);
        Page<BookReviewResponseDto> page = new PageImpl<>(List.of());
        BookReviewResponseDto review = mock(BookReviewResponseDto.class);
        when(jwtManager.getLoginMember(request)).thenReturn(member);
        when(bookReviewService.findAll("", 0, 9)).thenReturn(page);
        when(bookReviewService.findById(3L)).thenReturn(review);

        ExtendedModelMap listModel = new ExtendedModelMap();
        ExtendedModelMap detailModel = new ExtendedModelMap();

        assertThat(controller.list("", 0, listModel, request)).isEqualTo("bookreview/list");
        assertThat(controller.detail(3L, detailModel, request)).isEqualTo("bookreview/detail");
        assertThat(listModel.get("isAdmin")).isEqualTo(false);
        assertThat(detailModel.get("review")).isSameAs(review);
    }

    @Test
    void regularMemberCannotOpenWritePage() {
        when(jwtManager.getLoginMember(request)).thenReturn(member(Role.USER));

        assertThat(controller.write(new ExtendedModelMap(), request)).isEqualTo("error/404");

        verifyNoInteractions(bookReviewService);
    }

    @Test
    void administratorCanOpenWritePage() {
        when(jwtManager.getLoginMember(request)).thenReturn(member(Role.ADMIN));

        ExtendedModelMap model = new ExtendedModelMap();
        assertThat(controller.write(model, request)).isEqualTo("bookreview/form");
        assertThat(model.get("isAdmin")).isEqualTo(true);
    }

    private Member member(Role role) {
        return Member.builder()
                .id(role == Role.ADMIN ? 1L : 2L)
                .email(role.name().toLowerCase() + "@example.com")
                .pwd("password")
                .name(role.name())
                .role(role)
                .phone(role == Role.ADMIN ? "010-1111-1111" : "010-2222-2222")
                .birth("2000-01-01")
                .profile("profile.png")
                .build();
    }
}
