package spring.study.member.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import spring.study.mail.service.EmailVerificationService;
import spring.study.member.facade.RegistrationEmailFacade;
import spring.study.member.service.MemberService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RegistrationEmailControllerTest {
    @Test
    void registrationPageRequiresVerificationInTheSameBrowserSession() throws Exception {
        EmailVerificationService verification = mock(EmailVerificationService.class);
        var controller = new RegistrationEmailController(new RegistrationEmailFacade(verification, mock(MemberService.class)));
        var mvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(new InternalResourceViewResolver("/templates/", ".html"))
                .build();
        String email = "member@example.test";
        String attempt = "attempt";
        when(verification.sendRegistrationCode(email)).thenReturn(attempt);

        mvc.perform(get("/member/register"))
                .andExpect(redirectedUrl("/member/register/verify"));
        MockHttpSession session = (MockHttpSession) mvc.perform(post("/member/register/verify/send").param("email", email))
                .andExpect(redirectedUrl("/member/register/verify"))
                .andReturn().getRequest().getSession(false);

        mvc.perform(post("/member/register/verify").session(session).param("code", "a123A567"))
                .andExpect(redirectedUrl("/member/register"));
        verify(verification).verifyRegistrationCode(attempt, "a123A567");
        when(verification.verifiedRegistrationEmail(attempt)).thenReturn(email);
        mvc.perform(get("/member/register").session(session))
                .andExpect(view().name("member/register"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(model().attribute("email", email));
        mvc.perform(get("/member/register").session(new MockHttpSession()))
                .andExpect(redirectedUrl("/member/register/verify"));
        mvc.perform(get("/member/register/verify").session(session))
                .andExpect(redirectedUrl("/member/register"));

        mvc.perform(post("/member/register/verify/restart").session(session))
                .andExpect(redirectedUrl("/member/register/verify"));
        verify(verification).clearRegistration(attempt);
        mvc.perform(get("/member/register").session(session))
                .andExpect(redirectedUrl("/member/register/verify"));
    }
}
