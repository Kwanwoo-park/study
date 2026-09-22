package spring.study.regression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import spring.study.account.service.AccountService;
import spring.study.appeal.service.AppealService;
import spring.study.aws.entity.ImageCleanupTask;
import spring.study.aws.repository.ImageCleanupTaskRepository;
import spring.study.aws.service.ImageCleanupService;
import spring.study.aws.service.ImageS3Service;
import spring.study.aws.service.ImageUploadCleanupService;
import spring.study.board.service.BoardImgService;
import spring.study.board.service.BoardService;
import spring.study.bookreview.service.BookReviewService;
import spring.study.chat.facade.ChatFacade;
import spring.study.chat.repository.ChatMessageImgRepository;
import spring.study.chat.service.ChatMessageImgService;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatRoomService;
import spring.study.collection.service.CollectionService;
import spring.study.comment.service.CommentService;
import spring.study.common.service.ModerationService;
import spring.study.common.service.VisibilityAccessPolicy;
import spring.study.diary.dto.DiaryImageRequestDto;
import spring.study.diary.dto.DiaryRequestDto;
import spring.study.diary.entity.Diary;
import spring.study.diary.entity.DiaryImage;
import spring.study.diary.facade.DiaryFacade;
import spring.study.diary.repository.DiaryRepository;
import spring.study.diary.service.DiaryService;
import spring.study.favorite.service.FavoriteService;
import spring.study.follow.service.FollowService;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.jwt.service.JwtAuthenticationService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.facade.MemberFacade;
import spring.study.member.repository.MemberRepository;
import spring.study.member.service.MemberService;
import spring.study.member.service.UserService;
import spring.study.notification.service.NotificationService;
import spring.study.reply.service.ReplyService;
import spring.study.report.service.ReportService;
import spring.study.todo.service.TodoService;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"}, showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY, connection = EmbeddedDatabaseConnection.H2)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({ImageCleanupService.class, ImageUploadCleanupService.class, DiaryFacade.class, DiaryService.class, MemberFacade.class, MemberService.class, ChatFacade.class, ChatMessageImgService.class})
class ImageTransactionCleanupRegressionTest {
    private static final String OLD_IMAGE = "https://cdn/old.png";
    private static final String NEW_IMAGE = "https://cdn/new.png";
    @Autowired DiaryFacade diaryFacade;
    @Autowired MemberFacade memberFacade;
    @Autowired ChatFacade chatFacade;
    @Autowired ImageCleanupService cleanupService;
    @Autowired DiaryRepository diaryRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired ChatMessageImgRepository chatImageRepository;
    @Autowired ImageCleanupTaskRepository cleanupRepository;
    @Autowired PlatformTransactionManager transactionManager;
    @SpyBean ChatMessageImgService chatMessageImgService;
    @MockBean ImageS3Service imageS3Service;
    @MockBean BoardService boardService;
    @MockBean BoardImgService boardImgService;
    @MockBean CommentService commentService;
    @MockBean ReplyService replyService;
    @MockBean FollowService followService;
    @MockBean FavoriteService favoriteService;
    @MockBean ChatRoomMemberService roomMemberService;
    @MockBean ChatMessageService messageService;
    @MockBean ChatRoomService roomService;
    @MockBean CollectionService collectionService;
    @MockBean AccountService accountService;
    @MockBean UserService userService;
    @MockBean ForbiddenService forbiddenService;
    @MockBean NotificationService notificationService;
    @MockBean ReportService reportService;
    @MockBean AppealService appealService;
    @MockBean BookReviewService bookReviewService;
    @MockBean BCryptPasswordEncoder encoder;
    @MockBean JwtAuthenticationService jwtAuthenticationService;
    @MockBean TodoService todoService;
    @MockBean VisibilityAccessPolicy visibilityAccessPolicy;
    @MockBean ModerationService moderationService;
    @MockBean SimpMessagingTemplate messagingTemplate;
    private TransactionTemplate transaction;
    private Member member;

    @BeforeEach
    void setUp() {
        transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            diaryRepository.deleteAll();
            chatImageRepository.deleteAll();
            cleanupRepository.deleteAll();
            memberRepository.deleteAll();
        });
        member = memberRepository.saveAndFlush(Member.builder().email("image@test.com").pwd("pwd").name("member").role(Role.USER).phone("010-0000-0000").birth("2000-01-01").profile(OLD_IMAGE).build());
    }

    @Test
    void diaryDeleteCommitsCleanupTaskBeforeWorkerDeletesS3() {
        Diary diary = diaryWithImages(OLD_IMAGE);

        diaryFacade.delete(diary.getId(), member);

        assertThat(diaryRepository.existsById(diary.getId())).isFalse();
        assertCleanupUrls(OLD_IMAGE);
        verifyNoInteractions(imageS3Service);
        cleanupService.processNextBatch();
        verify(imageS3Service).deleteImage(OLD_IMAGE);
        assertCleanupUrls();
    }

    @Test
    void diaryDeleteRollbackPreservesImagesAndDiscardsCleanupTasks() {
        Diary diary = diaryWithImages(OLD_IMAGE);

        transaction.executeWithoutResult(status -> {
            diaryFacade.delete(diary.getId(), member);
            status.setRollbackOnly();
        });

        assertThat(diaryFacade.findDetail(diary.getId(), member).getImages()).hasSize(1);
        assertCleanupUrls();
        cleanupService.processNextBatch();
        verifyNoInteractions(imageS3Service);
    }

    @Test
    void diaryUpdateQueuesOnlyRemovedImages() {
        Diary diary = diaryWithImages(OLD_IMAGE, NEW_IMAGE);

        diaryFacade.update(diaryUpdate(diary.getId()), member);

        assertThat(diaryFacade.findDetail(diary.getId(), member).getImages()).extracting("imageUrl").containsExactly(NEW_IMAGE);
        assertCleanupUrls(OLD_IMAGE);
        verifyNoInteractions(imageS3Service);
    }

    @Test
    void diaryUpdateRollbackPreservesImagesAndDiscardsCleanupTasks() {
        Diary diary = diaryWithImages(OLD_IMAGE, NEW_IMAGE);

        transaction.executeWithoutResult(status -> {
            diaryFacade.update(diaryUpdate(diary.getId()), member);
            status.setRollbackOnly();
        });

        assertThat(diaryFacade.findDetail(diary.getId(), member).getImages()).hasSize(2);
        assertCleanupUrls();
        verifyNoInteractions(imageS3Service);
    }

    @Test
    void successfulProfileChangeQueuesOnlyTheOldImage() throws Exception {
        when(imageS3Service.uploadImageToS3(any())).thenReturn(NEW_IMAGE);

        assertThat(memberFacade.changeProfileImage(file("new.png"), member, new MockHttpServletRequest()).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(storedProfile()).isEqualTo(NEW_IMAGE);
        assertCleanupUrls(OLD_IMAGE);
        verify(imageS3Service, never()).deleteImage(anyString());
    }

    @Test
    void profileOuterRollbackQueuesOnlyTheNewImageInAnIndependentTransaction() throws Exception {
        when(imageS3Service.uploadImageToS3(any())).thenReturn(NEW_IMAGE);

        transaction.executeWithoutResult(status -> {
            memberFacade.changeProfileImage(file("new.png"), member, new MockHttpServletRequest());
            status.setRollbackOnly();
        });

        assertThat(storedProfile()).isEqualTo(OLD_IMAGE);
        assertCleanupUrls(NEW_IMAGE);
        cleanupService.processNextBatch();
        verify(imageS3Service).deleteImage(NEW_IMAGE);
        verify(imageS3Service, never()).deleteImage(OLD_IMAGE);
    }

    @Test
    void profileConstraintFailureDuringJpaCommitStillQueuesTheNewImage() throws Exception {
        String oversizedProfile = "https://cdn/" + "a".repeat(260) + ".png";
        when(imageS3Service.uploadImageToS3(any())).thenReturn(oversizedProfile);

        assertThatThrownBy(() -> memberFacade.changeProfileImage(file("new.png"), member, new MockHttpServletRequest())).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(storedProfile()).isEqualTo(OLD_IMAGE);
        assertCleanupUrls(oversizedProfile);
    }

    @Test
    void profileDatabaseUpdateFailureQueuesTheUploadedImage() throws Exception {
        when(imageS3Service.uploadImageToS3(any())).thenReturn(NEW_IMAGE);
        Member missingMember = Member.builder().id(-1L).profile(OLD_IMAGE).build();

        assertThat(memberFacade.changeProfileImage(file("new.png"), missingMember, new MockHttpServletRequest()).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        assertThat(storedProfile()).isEqualTo(OLD_IMAGE);
        assertCleanupUrls(NEW_IMAGE);
    }

    @Test
    void failedProfileUploadNeverQueuesTheOldImage() throws Exception {
        when(imageS3Service.uploadImageToS3(any())).thenThrow(new IOException("upload failed"));

        assertThat(memberFacade.changeProfileImage(file("new.png"), member, new MockHttpServletRequest()).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        assertThat(storedProfile()).isEqualTo(OLD_IMAGE);
        assertCleanupUrls();
    }

    @Test
    void successfulChatUploadPreservesAllImages() throws Exception {
        when(imageS3Service.uploadImageToS3(any())).thenReturn(OLD_IMAGE, NEW_IMAGE);

        assertThat(chatFacade.sendImage(List.of(file("first.png"), file("second.png"))).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(chatImageRepository.count()).isEqualTo(2);
        assertCleanupUrls();
        verify(imageS3Service, never()).deleteImage(anyString());
    }

    @Test
    void partialChatUploadFailureQueuesOnlySuccessfulUploads() throws Exception {
        when(imageS3Service.uploadImageToS3(any())).thenReturn(NEW_IMAGE).thenThrow(new IOException("second upload failed"));

        assertThat(chatFacade.sendImage(List.of(file("first.png"), file("second.png"))).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        assertThat(chatImageRepository.count()).isZero();
        assertCleanupUrls(NEW_IMAGE);
    }

    @Test
    void chatDatabaseFailureRollsBackMetadataAndQueuesUploadedFiles() throws Exception {
        when(imageS3Service.uploadImageToS3(any())).thenReturn(NEW_IMAGE);
        doThrow(new IllegalStateException("save failed")).when(chatMessageImgService).saveAll(anyList());

        assertThat(chatFacade.sendImage(List.of(file("new.png"))).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        assertThat(chatImageRepository.count()).isZero();
        assertCleanupUrls(NEW_IMAGE);
    }

    @Test
    void chatCommitFailureRollsBackMetadataAndQueuesUploadedFiles() throws Exception {
        when(imageS3Service.uploadImageToS3(any())).thenAnswer(invocation -> {
            failDuringCommit();
            return NEW_IMAGE;
        });

        assertThatThrownBy(() -> chatFacade.sendImage(List.of(file("new.png")))).isInstanceOf(IllegalStateException.class).hasMessage("commit failed");

        assertThat(chatImageRepository.count()).isZero();
        assertCleanupUrls(NEW_IMAGE);
    }

    @Test
    void chatOuterRollbackQueuesUploadedFilesAndRollsBackMetadata() throws Exception {
        when(imageS3Service.uploadImageToS3(any())).thenReturn(NEW_IMAGE);

        transaction.executeWithoutResult(status -> {
            chatFacade.sendImage(List.of(file("new.png")));
            status.setRollbackOnly();
        });

        assertThat(chatImageRepository.count()).isZero();
        assertCleanupUrls(NEW_IMAGE);
    }

    @Test
    void cleanupWorkerRetriesAnS3FailureWithoutLosingTheTask() {
        cleanupService.enqueue(NEW_IMAGE);
        doThrow(new IllegalStateException("S3 unavailable")).doNothing().when(imageS3Service).deleteImage(NEW_IMAGE);

        cleanupService.processNextBatch();

        assertCleanupUrls(NEW_IMAGE);
        assertThat(cleanupRepository.findAll().get(0).getAttemptCount()).isEqualTo(1);
        cleanupService.processNextBatch();
        assertCleanupUrls();
        verify(imageS3Service, times(2)).deleteImage(NEW_IMAGE);
    }

    private Diary diaryWithImages(String... urls) {
        Diary diary = Diary.builder().member(member).title("diary").content("content").build();
        for (String url : urls) diary.addImage(DiaryImage.builder().imageUrl(url).build());
        return diaryRepository.saveAndFlush(diary);
    }

    private DiaryRequestDto diaryUpdate(Long id) {
        return DiaryRequestDto.builder().id(id).title("updated").content("updated").images(List.of(DiaryImageRequestDto.builder().imageUrl(NEW_IMAGE).build())).build();
    }

    private MockMultipartFile file(String filename) {
        return new MockMultipartFile("file", filename, "image/png", new byte[] {1});
    }

    private String storedProfile() {
        return memberRepository.findById(member.getId()).orElseThrow().getProfile();
    }

    private void assertCleanupUrls(String... urls) {
        assertThat(cleanupRepository.findAll()).extracting(ImageCleanupTask::getImageUrl).containsExactlyInAnyOrder(urls);
    }

    private void failDuringCommit() {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void beforeCommit(boolean readOnly) {
                throw new IllegalStateException("commit failed");
            }
        });
    }
}
