package spring.study.bookreview.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import spring.study.admin.service.SystemIncidentService;
import spring.study.bookreview.entity.BookReview;
import spring.study.bookreview.facade.BookReviewFacade;
import spring.study.bookreview.repository.BookReviewRepository;
import spring.study.bookreview.service.BookReviewService;
import spring.study.common.component.GlobalExceptionHandler;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookReviewApiControllerTest {
    private static final String BODY = """
            {"reviewTitle":"독후감","bookTitle":"책","bookAuthor":"저자","rating":5,"content":"독후감 내용"}
            """;
    private final BookReviewRepository repository = mock(BookReviewRepository.class);
    private final JwtManager jwtManager = mock(JwtManager.class);
    private final Member admin = Member.builder().id(1L).email("admin@example.test").name("관리자").role(Role.ADMIN).build();
    private final Member user = Member.builder().id(2L).email("user@example.test").role(Role.USER).build();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new BookReviewApiController(
                        new BookReviewFacade(new BookReviewService(repository)), jwtManager, new CommonFacade()))
                .setControllerAdvice(new GlobalExceptionHandler(mock(SystemIncidentService.class))).build();
    }

    @Test
    void unauthenticatedRequestsAreStillRejectedBeforeRepositoryAccess() throws Exception {
        mvc.perform(get("/api/book-reviews")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/book-reviews/4")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/book-reviews").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
    }

    @Test
    void readersReceiveTheExistingPageAndDetailResponseShapes() throws Exception {
        when(jwtManager.getLoginMember(any())).thenReturn(user);
        when(repository.search(eq("책"), any())).thenReturn(new PageImpl<>(List.of(review()), PageRequest.of(0, 1), 2));
        when(repository.findById(4L)).thenReturn(Optional.of(review()));

        mvc.perform(get("/api/book-reviews?keyword=책&page=0&size=1"))
                .andExpect(status().isOk()).andExpect(jsonPath("result").value(10))
                .andExpect(jsonPath("reviews[0].id").value(4)).andExpect(jsonPath("totalElements").value(2))
                .andExpect(jsonPath("totalPages").value(2)).andExpect(jsonPath("hasNext").value(true));
        mvc.perform(get("/api/book-reviews/4"))
                .andExpect(status().isOk()).andExpect(jsonPath("review.id").value(4));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PATCH", "DELETE"})
    void regularMembersCannotWriteEvenThroughTheNewFacade(String method) throws Exception {
        when(jwtManager.getLoginMember(any())).thenReturn(user);
        var request = switch (method) {
            case "POST" -> post("/api/book-reviews");
            case "PATCH" -> patch("/api/book-reviews/4");
            default -> delete("/api/book-reviews/4");
        };

        mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        verifyNoInteractions(repository);
    }

    @Test
    void administratorCanCreateUpdateAndDeleteWithUnchangedResponses() throws Exception {
        when(jwtManager.getLoginMember(any())).thenReturn(admin);
        BookReview review = review();
        when(repository.save(any())).thenReturn(review);
        when(repository.findById(4L)).thenReturn(Optional.of(review));

        mvc.perform(post("/api/book-reviews").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("result").value(4))
                .andExpect(jsonPath("message").value("독후감이 등록되었습니다"));
        mvc.perform(patch("/api/book-reviews/4").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("result").value(4))
                .andExpect(jsonPath("message").value("독후감이 수정되었습니다"));
        mvc.perform(delete("/api/book-reviews/4"))
                .andExpect(status().isOk()).andExpect(jsonPath("result").value(4))
                .andExpect(jsonPath("message").value("독후감이 삭제되었습니다"));
        verify(repository).delete(review);
    }

    @Test
    void invalidInputDoesNotReachTheRepository() throws Exception {
        mvc.perform(post("/api/book-reviews").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("result").value(-10));
        verifyNoInteractions(repository);
    }

    private BookReview review() {
        return BookReview.builder().id(4L).author(admin).reviewTitle("독후감").bookTitle("책")
                .bookAuthor("저자").rating(5).content("독후감 내용").build();
    }
}
