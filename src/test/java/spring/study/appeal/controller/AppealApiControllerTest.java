package spring.study.appeal.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import spring.study.appeal.dto.AppealResponseDto;
import spring.study.appeal.entity.Appeal;
import spring.study.appeal.facade.AppealFacade;
import spring.study.appeal.service.AppealService;
import spring.study.appeal.service.AppealVerificationService;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AppealApiControllerTest {
    private final JwtManager jwtManager = mock(JwtManager.class);
    private final AppealService service = mock(AppealService.class);
    private final AppealVerificationService verificationService = mock(AppealVerificationService.class);
    private final Member member = Member.builder().id(7L).email("appeal@example.test").build();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AppealApiController(
                new AppealFacade(service, verificationService), jwtManager, new CommonFacade())).build();
    }

    @Test
    void contextRequiresLoginAndReturnsOnlyTheAuthenticatedMembersContext() throws Exception {
        mvc.perform(get("/api/appeal/context")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);

        when(jwtManager.getLoginMember(any())).thenReturn(member);
        when(service.findSanctions(member)).thenReturn(List.of());
        when(service.findByMember(member)).thenReturn(List.of());
        mvc.perform(get("/api/appeal/context?email=someone-else@example.test"))
                .andExpect(status().isOk()).andExpect(jsonPath("result").value(10))
                .andExpect(jsonPath("memberEmail").value(member.getEmail()))
                .andExpect(jsonPath("memberName").value(""))
                .andExpect(jsonPath("sanctions").isArray()).andExpect(jsonPath("appeals").isArray());
    }

    @Test
    void guestVerificationKeepsFiveMinuteExpiryAndOneTimeTokenResponse() throws Exception {
        mvc.perform(post("/api/appeal/verification/send").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"appeal@example.test\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("expiresInSeconds").value(300));
        verify(verificationService).sendCode("appeal@example.test");

        when(verificationService.verifyCode("appeal@example.test", "123456")).thenReturn("verification-test-token");
        mvc.perform(post("/api/appeal/verification/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"appeal@example.test\",\"code\":\"123456\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("expiresInSeconds").value(300))
                .andExpect(jsonPath("verificationToken").value("verification-test-token"));
    }

    @Test
    void invalidInputDoesNotReachVerificationOrSubmissionServices() throws Exception {
        mvc.perform(post("/api/appeal/verification/send").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalid\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("result").value(-10));
        mvc.perform(post("/api/appeal").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("result").value(-10));
        verifyNoInteractions(service, verificationService);
    }

    @Test
    void guestSubmissionPassesVerificationProofToTheServiceWithoutInventingALogin() throws Exception {
        when(service.create(any(), isNull())).thenReturn(new AppealResponseDto(Appeal.builder()
                .id(19L).member(member).title("상소").content("상소 내용").build()));

        mvc.perform(post("/api/appeal").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"appeal@example.test","verificationToken":"proof","title":"상소","content":"상소 내용"}
                """))
                .andExpect(status().isOk()).andExpect(jsonPath("result").value(19))
                .andExpect(jsonPath("appeal.memberEmail").value(member.getEmail()));
        verify(service).create(argThat(dto -> "proof".equals(dto.getVerificationToken())), isNull());
    }

    @Test
    void serviceFailuresKeepTheirStatusAndFallbackMessage() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS)).when(verificationService).sendCode(anyString());

        mvc.perform(post("/api/appeal/verification/send").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"appeal@example.test\"}"))
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("result").value(-10))
                .andExpect(jsonPath("message").value("인증번호를 발송할 수 없습니다"));
    }
}
