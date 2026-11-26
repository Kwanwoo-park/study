package spring.study.appeal.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import spring.study.appeal.entity.Appeal;
import spring.study.appeal.facade.AppealFacade;
import spring.study.appeal.service.AppealService;
import spring.study.mail.entity.EmailVerificationPurpose;
import spring.study.mail.service.EmailVerificationService;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;
import spring.study.sanction.service.MemberSanctionService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AppealApiControllerTest {
    private final JwtManager jwtManager = mock(JwtManager.class);
    private final AppealService service = mock(AppealService.class);
    private final EmailVerificationService verificationService = mock(EmailVerificationService.class);
    private final spring.study.member.service.MemberService memberService = mock(spring.study.member.service.MemberService.class);
    private final MemberSanctionService sanctions = mock(MemberSanctionService.class);
    private final Member member = Member.builder().id(7L).email("appeal@example.test").build();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AppealApiController(
                new AppealFacade(service, verificationService, memberService, sanctions,
                        mock(spring.study.notification.service.NotificationService.class),
                        new spring.study.common.component.TransactionRunner(mock(org.springframework.transaction.PlatformTransactionManager.class))),
                jwtManager, new CommonFacade())).build();
    }

    @Test
    void guestSubmissionPassesVerificationProofToTheServiceWithoutInventingALogin() throws Exception {
        when(memberService.findMemberOrNull(member.getEmail())).thenReturn(member);
        when(verificationService.consume(EmailVerificationPurpose.APPEAL, "7", member.getEmail(), "proof")).thenReturn(true);
        when(service.create(any(), eq(member), isNull())).thenReturn(Appeal.builder()
                .id(19L).member(member).title("상소").content("상소 내용").build());

        mvc.perform(post("/api/appeal").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"appeal@example.test","verificationToken":"proof","title":"상소","content":"상소 내용"}
                """))
                .andExpect(status().isOk()).andExpect(jsonPath("result").value(19))
                .andExpect(jsonPath("appeal.memberEmail").value(member.getEmail()));
        verify(verificationService).consume(EmailVerificationPurpose.APPEAL, "7", member.getEmail(), "proof");
        verify(service).create(argThat(dto -> "proof".equals(dto.getVerificationToken())), eq(member), isNull());
    }

}
