package spring.study.regression;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import spring.study.appeal.controller.AppealApiController;
import spring.study.appeal.facade.AppealFacade;
import spring.study.board.controller.BoardApiController;
import spring.study.board.facade.BoardFacade;
import spring.study.bookreview.controller.BookReviewApiController;
import spring.study.bookreview.facade.BookReviewFacade;
import spring.study.chat.controller.ChatApiController;
import spring.study.chat.controller.ChatRoomDetailsController;
import spring.study.chat.controller.ChatWebSocketApiController;
import spring.study.chat.facade.*;
import spring.study.collection.controller.CollectionApiController;
import spring.study.collection.facade.CollectionFacade;
import spring.study.comment.controller.CommentApiController;
import spring.study.comment.facade.CommentFacade;
import spring.study.common.facade.CommonFacade;
import spring.study.diary.controller.DiaryApiController;
import spring.study.diary.facade.DiaryFacade;
import spring.study.favorite.controller.FavoriteApiController;
import spring.study.favorite.facade.FavoriteFacade;
import spring.study.follow.controller.FollowApiController;
import spring.study.follow.facade.FollowFacade;
import spring.study.forbidden.controller.ForbiddenApiController;
import spring.study.forbidden.facade.ForbiddenFacade;
import spring.study.jwt.controller.MobileAuthController;
import spring.study.jwt.controller.MobileOAuthController;
import spring.study.jwt.facade.MobileAuthFacade;
import spring.study.jwt.facade.MobileOAuthFacade;
import spring.study.mail.controller.MailApiController;
import spring.study.mail.facade.MailFacade;
import spring.study.member.controller.MemberApiController;
import spring.study.member.facade.MemberAuthFacade;
import spring.study.member.facade.MemberFacade;
import spring.study.notification.controller.NotificationApiController;
import spring.study.notification.facade.NotificationFacade;
import spring.study.reply.controller.ReplyApiController;
import spring.study.reply.facade.ReplyFacade;
import spring.study.report.controller.ReportApiController;
import spring.study.report.facade.ReportFacade;
import spring.study.todo.controller.TodoApiController;
import spring.study.todo.controller.TodoViewController;
import spring.study.todo.facade.TodoFacade;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

class ControllerFacadeWiringRegressionTest {
    @Test
    void springConnectsRealControllersAndFacadesWithoutMissingBeansOrCycles() {
        List<Class<?>> applicationTypes = List.of(
                CommonFacade.class, AppealApiController.class, AppealFacade.class,
                BoardApiController.class, BoardFacade.class, BookReviewApiController.class, BookReviewFacade.class,
                ChatApiController.class, ChatRoomDetailsController.class, ChatWebSocketApiController.class, ChatFacade.class, ChatSendFacade.class,
                ChatViewFacade.class, ChatRoomFacade.class, AudioCallFacade.class,
                CollectionApiController.class, CollectionFacade.class, CommentApiController.class, CommentFacade.class,
                DiaryApiController.class, DiaryFacade.class, FavoriteApiController.class, FavoriteFacade.class,
                FollowApiController.class, FollowFacade.class, ForbiddenApiController.class, ForbiddenFacade.class,
                MobileAuthController.class, MobileOAuthController.class, MobileAuthFacade.class, MobileOAuthFacade.class,
                MailApiController.class, MailFacade.class, MemberApiController.class, MemberFacade.class, MemberAuthFacade.class,
                NotificationApiController.class, NotificationFacade.class, ReplyApiController.class, ReplyFacade.class,
                ReportApiController.class, ReportFacade.class,
                TodoApiController.class, TodoViewController.class, TodoFacade.class
        );

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(applicationTypes.toArray(Class<?>[]::new));
            // Keep the entire controller/facade graph real; isolate only its service/infrastructure boundary.
            applicationTypes.stream()
                    .flatMap(type -> Arrays.stream(type.getDeclaredConstructors()))
                    .flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
                    .filter(type -> !applicationTypes.contains(type))
                    .distinct()
                    .forEach(type -> registerDependency(context, type));

            context.refresh();

            for (Class<?> type : applicationTypes) {
                Object bean = context.getBean(type);
                assertThat(bean).isInstanceOf(type);
                assertThat(mockingDetails(bean).isMock()).isFalse();
            }
        }
    }

    private <T> void registerDependency(AnnotationConfigApplicationContext context, Class<T> type) {
        // These boundary mocks are already initialized and must not consume production @Value settings.
        context.getBeanFactory().registerSingleton(type.getName(), mock(type));
    }
}
