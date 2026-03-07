package spring.study.regression;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import spring.study.aws.service.ImageS3Service;
import spring.study.board.service.BoardImgService;
import spring.study.board.service.BoardService;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.comment.service.CommentService;
import spring.study.common.service.SessionManager;
import spring.study.favorite.service.FavoriteService;
import spring.study.follow.service.FollowService;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.facade.MemberFacade;
import spring.study.member.service.MemberService;
import spring.study.member.service.UserService;
import spring.study.notification.service.NotificationService;
import spring.study.reply.service.ReplyService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberFacadeRegressionTest {

    @Mock private SessionManager sessionManager;
    @Mock private MemberService memberService;
    @Mock private BoardService boardService;
    @Mock private BoardImgService boardImgService;
    @Mock private CommentService commentService;
    @Mock private ReplyService replyService;
    @Mock private FollowService followService;
    @Mock private FavoriteService favoriteService;
    @Mock private ChatRoomMemberService roomMemberService;
    @Mock private ChatMessageService messageService;
    @Mock private UserService userService;
    @Mock private ForbiddenService forbiddenService;
    @Mock private NotificationService notificationService;
    @Mock private ImageS3Service imageS3Service;
    @Mock private BCryptPasswordEncoder encoder;

    @InjectMocks
    private MemberFacade memberFacade;

    @Test
    void changeProfileImageShouldDeleteOldImageOnlyAfterSuccessfulUpload() throws Exception {
        Member member = Member.builder()
                .id(1L)
                .email("a@test.com")
                .pwd("pwd")
                .name("name")
                .role(Role.USER)
                .phone("010-1111-2222")
                .birth("2000-01-01")
                .profile("https://cdn/old.png")
                .build();

        MockMultipartFile file = new MockMultipartFile("file", "new.png", "image/png", "img".getBytes());
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);

        when(request.getSession(false)).thenReturn(session);
        when(imageS3Service.uploadImageToS3(file)).thenReturn("https://cdn/new.png");

        var response = memberFacade.changeProfileImage(file, member, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        InOrder inOrder = inOrder(imageS3Service, memberService);
        inOrder.verify(imageS3Service).uploadImageToS3(file);
        inOrder.verify(memberService).updateProfile(1L, "https://cdn/new.png");
        inOrder.verify(imageS3Service).deleteImage("https://cdn/old.png");

        verify(session).setAttribute("member", member);
    }

    @Test
    void changeProfileImageShouldNotDeleteOldImageWhenUploadFails() throws Exception {
        Member member = Member.builder()
                .id(1L)
                .email("a@test.com")
                .pwd("pwd")
                .name("name")
                .role(Role.USER)
                .phone("010-1111-2222")
                .birth("2000-01-01")
                .profile("https://cdn/old.png")
                .build();

        MockMultipartFile file = new MockMultipartFile("file", "new.png", "image/png", "img".getBytes());
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(imageS3Service.uploadImageToS3(file)).thenThrow(new IOException("upload failed"));

        var response = memberFacade.changeProfileImage(file, member, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(imageS3Service, never()).deleteImage(anyString());
        verify(memberService, never()).updateProfile(anyLong(), anyString());
    }

    @Test
    void duplicateCheckShouldReturnConflictWhenEmailExists() {
        when(memberService.existEmail("dup@test.com")).thenReturn(true);

        var response = memberFacade.duplicateCheck("dup@test.com");

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void updatePhoneShouldReturnConflictOnDuplicatePhoneAndBeSessionSafe() {
        MemberRequestDto dto = MemberRequestDto.builder()
                .email("a@test.com")
                .phone("010-9999-8888")
                .birth("2000-01-01")
                .build();

        Member member = Member.builder()
                .id(1L)
                .email("a@test.com")
                .pwd("pwd")
                .name("name")
                .role(Role.USER)
                .phone("010-1111-2222")
                .birth("2000-01-01")
                .profile("https://cdn/old.png")
                .build();

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(memberService.findMember("a@test.com")).thenReturn(member);
        when(memberService.updatePhoneAndBirth(1L, "010-9999-8888", "2000-01-01")).thenReturn(-2);

        var response = memberFacade.updatePhone(dto, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }
}
