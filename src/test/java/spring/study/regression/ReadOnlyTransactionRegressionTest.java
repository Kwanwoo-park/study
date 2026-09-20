package spring.study.regression;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;
import spring.study.jwt.service.RefreshTokenService;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReadOnlyTransactionRegressionTest {
    private final AnnotationTransactionAttributeSource attributes = new AnnotationTransactionAttributeSource();

    // Audited database reads only: cache, HTTP, S3 and mutation methods are not read transactions.
    private static final Map<String, Set<String>> SERVICE_READS = Map.ofEntries(
            Map.entry("account.service.AccountService", Set.of("findByAccount", "findByMember", "findActiveByMember", "findAll", "existsByAccount", "hasActiveSavingsUsingSource")),
            Map.entry("account.service.AccountTransactionService", Set.of("findByAccount", "findByWithdrawalAccount", "findByDepositAccount")),
            Map.entry("account.service.SavingsAutoTransferService", Set.of("findDueSavingsAccountNumbers")),
            Map.entry("admin.service.AdminFileService", Set.of("list")),
            Map.entry("appeal.service.AppealService", Set.of("findSanctions", "findByMember", "findAll")),
            Map.entry("board.service.BoardService", Set.of("findNewBoard", "getBoard", "getBoardByMember", "findAll", "findByMember", "findById", "existBoard", "countByMember", "countByMembers", "getBoardIdList")),
            Map.entry("board.service.BoardImgService", Set.of("findBoard")),
            Map.entry("chat.service.ChatMessageService", Set.of("findById", "find", "findActiveChatting", "loadChatting", "countUnread", "findLatestVisible")),
            Map.entry("chat.service.ChatMessageImgService", Set.of("findMessage", "findMessageImg")),
            Map.entry("chat.service.ChatRoomService", Set.of("findAll", "find", "findByName")),
            Map.entry("chat.service.ChatRoomMemberService", Set.of("exist", "find", "findMember")),
            Map.entry("collection.service.CollectionService", Set.of("getCollections", "findAll", "findByMember", "findById", "countByMember")),
            Map.entry("comment.service.CommentService", Set.of("findAll", "findById", "getComments", "existComment", "countComments")),
            Map.entry("common.service.VisibilityAccessPolicy", Set.of("canViewMember", "canViewBoard")),
            Map.entry("favorite.service.FavoriteService", Set.of("findByBoard", "findByMember", "findByMemberAndBoard", "existFavorite", "getFavorites", "countFavorites", "findLikedBoardIds")),
            Map.entry("follow.service.FollowService", Set.of("findFollow", "existFollow", "getMemberList", "findByFollower", "getFollowers", "getVisibleFollowers", "getFollowing", "getVisibleFollowing", "countVisibleFollowers", "countVisibleFollowing", "countFollowers", "countFollowing", "findAll")),
            Map.entry("forbidden.service.ForbiddenService", Set.of("findAll", "findByWord", "findByRisk", "findWordList", "findByStatus", "findByStatusNot", "existWord")),
            Map.entry("member.service.MemberService", Set.of("findAll", "findById", "findName", "findMember", "findAdministrator", "existEmail", "findNewUser", "loadUserByUsername")),
            Map.entry("notification.service.NotificationService", Set.of("countUnReadNotification", "findById", "findByMember", "findByMemberAndGroup", "countByMember", "countByMemberAndGroup", "findUnReadNotification")),
            Map.entry("reply.service.ReplyService", Set.of("findById", "findReply", "getReplies", "countReplies")),
            Map.entry("report.service.ReportService", Set.of("findById", "findByReporter", "findAll", "findHistory", "findAllByStatus"))
    );

    @Test
    void serviceQueriesAreReadOnlyAndOtherOperationsStayWritable() throws Exception {
        for (var entry : SERVICE_READS.entrySet()) {
            Class<?> type = Class.forName("spring.study." + entry.getKey());
            for (String name : entry.getValue()) {
                assertThat(type.getDeclaredMethods()).anyMatch(method -> method.getName().equals(name));
            }
            for (Method method : type.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers()) || method.isSynthetic()) continue;
                TransactionAttribute transaction = attributes.getTransactionAttribute(method, type);
                boolean query = entry.getValue().contains(method.getName());
                // This overload removes the comment from its member and board.
                if (type.getSimpleName().equals("CommentService") && method.getName().equals("findById")
                        && method.getParameterCount() == 3) query = false;
                // This overload only checks supplied values; it does not query the database.
                if (type.getSimpleName().equals("VisibilityAccessPolicy") && method.getName().equals("canViewBoard")
                        && method.getParameterCount() == 3) query = false;

                if (query) {
                    assertThat(transaction).as(method.toGenericString()).isNotNull();
                    assertThat(transaction.isReadOnly()).as(method.toGenericString()).isTrue();
                    assertThat(transaction.getPropagationBehavior()).as(method.toGenericString())
                            .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
                } else if (transaction != null) {
                    assertThat(transaction.isReadOnly()).as(method.toGenericString()).isFalse();
                }
            }
        }
    }

    @Test
    void declaredJpaQueriesAreReadOnlyExceptLocksAndMutations() throws Exception {
        var resolver = new PathMatchingResourcePatternResolver();
        var readerFactory = new CachingMetadataReaderFactory(resolver);
        int queryCount = 0;
        for (var resource : resolver.getResources("classpath*:spring/study/**/repository/*Repository.class")) {
            Class<?> type = Class.forName(readerFactory.getMetadataReader(resource).getClassMetadata().getClassName());
            if (!JpaRepository.class.isAssignableFrom(type)) continue;
            for (Method method : type.getDeclaredMethods()) {
                if (method.isSynthetic()) continue;
                TransactionAttribute transaction = attributes.getTransactionAttribute(method, type);
                boolean query = method.getName().matches("^(find|count|exists|search).*")
                        && !method.isAnnotationPresent(Lock.class)
                        && !method.isAnnotationPresent(Modifying.class);
                if (query) {
                    queryCount++;
                    assertThat(transaction).as(method.toGenericString()).isNotNull();
                    assertThat(transaction.isReadOnly()).as(method.toGenericString()).isTrue();
                    assertThat(transaction.getPropagationBehavior()).as(method.toGenericString())
                            .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
                } else if (transaction != null) {
                    assertThat(transaction.isReadOnly()).as(method.toGenericString()).isFalse();
                }
            }
        }
        assertThat(queryCount).isPositive();
    }

    @Test
    void externalIpLookupKeepsExistingTransactionSuspension() throws Exception {
        Method method = RefreshTokenService.class.getMethod("findActiveSessions");
        TransactionAttribute transaction = attributes.getTransactionAttribute(method, RefreshTokenService.class);
        assertThat(transaction).isNotNull();
        assertThat(transaction.isReadOnly()).isTrue();
        assertThat(transaction.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
    }
}

