package spring.study.regression;

import jakarta.persistence.LockModeType;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountStatus;
import spring.study.account.entity.AccountTransaction;
import spring.study.account.entity.AccountTransactionStatus;
import spring.study.account.entity.AccountTransactionType;
import spring.study.account.entity.AccountType;
import spring.study.account.repository.AccountRepository;
import spring.study.account.repository.AccountTransactionRepository;
import spring.study.account.repository.SavingAutoTransferRepository;
import spring.study.account.service.SavingAutoTransferService;
import spring.study.board.entity.Board;
import spring.study.common.dto.EntityIdProjection;
import spring.study.favorite.dto.FavoriteBoardIdProjection;
import spring.study.favorite.entity.Favorite;
import spring.study.favorite.repository.FavoriteRepository;
import spring.study.jwt.dto.RefreshTokenMemberIdProjection;
import spring.study.jwt.entity.RefreshToken;
import spring.study.jwt.repository.RefreshTokenRepository;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.repository.MemberRepository;
import spring.study.notification.entity.Notification;
import spring.study.notification.entity.Status;
import spring.study.notification.repository.NotificationRepository;
import spring.study.report.entity.Report;
import spring.study.report.entity.ReportReason;
import spring.study.report.entity.ReportTargetType;
import spring.study.report.repository.ReportRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop", showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY, connection = EmbeddedDatabaseConnection.H2)
class RepositoryQueryDerivationTest {
    @Autowired TestEntityManager entityManager;
    @Autowired AccountRepository accounts;
    @Autowired SavingAutoTransferRepository autoTransfers;
    @Autowired AccountTransactionRepository transactions;
    @Autowired FavoriteRepository favorites;
    @Autowired RefreshTokenRepository tokens;
    @Autowired MemberRepository members;
    @Autowired ReportRepository reports;
    @Autowired NotificationRepository notifications;

    @Test
    void favoriteProjectionTraversesBoardIdAndKeepsMemberAndBoardFilters() {
        Member viewer = member("viewer", null);
        Member other = member("other", null);
        Board selected = entityManager.persist(Board.builder().content("selected").member(other).build());
        Board excluded = entityManager.persist(Board.builder().content("excluded").member(other).build());
        favorites.save(Favorite.builder().member(viewer).board(selected).build());
        favorites.save(Favorite.builder().member(viewer).board(excluded).build());
        favorites.saveAndFlush(Favorite.builder().member(other).board(selected).build());
        entityManager.clear();

        assertThat(favorites.findByMemberAndBoard_IdIn(viewer, List.of(selected.getId())))
                .extracting(FavoriteBoardIdProjection::boardId).containsExactly(selected.getId());
    }

    @Test
    void refreshTokenProjectionReturnsDistinctMemberIdsRatherThanDistinctTokens() {
        tokens.save(new RefreshToken("token-1", 1L, Duration.ofDays(1)));
        tokens.save(new RefreshToken("token-2", 1L, Duration.ofDays(1)));
        tokens.saveAndFlush(new RefreshToken("token-3", 2L, Duration.ofDays(1)));

        assertThat(tokens.findDistinctBy()).extracting(RefreshTokenMemberIdProjection::memberId)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void inactiveMemberQueryRestrictsBothOrBranchesAndIncludesTheCutoff() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 10, 20, 12, 0);
        Member neverLoggedIn = member("never", null);
        Member atCutoff = member("cutoff", cutoff);
        Member recent = member("recent", cutoff.plusSeconds(1));
        member("unrequested-never", null);
        member("unrequested-old", cutoff.minusDays(1));
        List<Long> requestedIds = List.of(neverLoggedIn.getId(), atCutoff.getId(), recent.getId());

        assertThat(members.findByIdInAndLastLoginTimeIsNullOrIdInAndLastLoginTimeLessThanEqual(
                requestedIds, requestedIds, cutoff)).extracting(EntityIdProjection::id)
                .containsExactlyInAnyOrder(neverLoggedIn.getId(), atCutoff.getId());
        assertThat(members.findByIdInAndLastLoginTimeIsNullOrIdInAndLastLoginTimeLessThanEqual(
                List.of(), List.of(), cutoff)).isEmpty();
    }

    @Test
    void accountQueriesTraverseSubtypeRowsAndResolveTheSourceUsingThePrimaryKey() {
        Account source = accounts.saveAndFlush(account("source", AccountType.DEPOSIT_WITHDRAWAL));
        Account unrelated = accounts.saveAndFlush(account("unrelated", AccountType.DEPOSIT_WITHDRAWAL));
        Account savings = account("savings", AccountType.INSTALLMENT_SAVINGS);
        savings.configureSavingsAutoTransfer(source, 100_000L, 10, LocalDate.of(2026, 1, 1));
        accounts.saveAndFlush(savings);
        LocalDateTime cutoff = LocalDateTime.of(2026, 11, 4, 12, 0);
        Account deposit = account("deposit", AccountType.TIME_DEPOSIT);
        deposit.setMaturityAt(cutoff);
        accounts.saveAndFlush(deposit);
        entityManager.clear();

        assertThat(accounts.existsBySavingsAutoTransfer_SourceAccountAndAccountStatusIn(source, List.of(AccountStatus.ACTIVE))).isTrue();
        assertThat(accounts.existsBySavingsAutoTransfer_SourceAccountAndAccountStatusIn(unrelated, List.of(AccountStatus.ACTIVE))).isFalse();
        assertThat(accounts.findByAccountStatusAndInterestDetail_MaturityAtLessThanEqual(AccountStatus.ACTIVE, cutoff))
                .extracting(Account::getAccount).containsExactly(deposit.getAccount());
        assertThat(accounts.findByAccountStatusAndInterestDetail_MaturityAtLessThanEqual(AccountStatus.ACTIVE, cutoff.minusSeconds(1))).isEmpty();
        SavingAutoTransferService service = new SavingAutoTransferService(autoTransfers);
        assertThat(service.findSavingsSourceAccountNumber(savings.getAccount())).isEqualTo(source.getAccount());
        assertThat(service.findSavingsSourceAccountNumber("missing")).isNull();
        assertThat(service.findSavingsSourceAccountNumber(null)).isNull();
    }

    @Test
    void entityGraphsKeepNullableTransactionAccountsAndPessimisticLocks() {
        Account account = accounts.saveAndFlush(account("checking", AccountType.DEPOSIT_WITHDRAWAL));
        AccountTransaction deposit = transactions.save(AccountTransaction.builder()
                .depositAccount(account).transactionType(AccountTransactionType.DEPOSIT)
                .transactionStatus(AccountTransactionStatus.COMPLETED).amount(1000L)
                .transactionTime(LocalDateTime.now()).build());
        AccountTransaction withdrawal = transactions.saveAndFlush(AccountTransaction.builder()
                .withdrawalAccount(account).transactionType(AccountTransactionType.WITHDRAWAL)
                .transactionStatus(AccountTransactionStatus.COMPLETED).amount(1000L)
                .transactionTime(LocalDateTime.now()).build());
        entityManager.clear();

        AccountTransaction loadedDeposit = transactions.findForUpdateById(deposit.getId()).orElseThrow();
        AccountTransaction loadedWithdrawal = transactions.findForUpdateById(withdrawal.getId()).orElseThrow();
        assertThat(loadedDeposit.getWithdrawalAccount()).isNull();
        assertThat(loadedWithdrawal.getDepositAccount()).isNull();
        assertThat(Hibernate.isInitialized(loadedDeposit.getDepositAccount())).isTrue();
        assertThat(Hibernate.isInitialized(loadedWithdrawal.getWithdrawalAccount())).isTrue();
        assertLocked(loadedDeposit);
        assertLocked(loadedWithdrawal);
    }

    @Test
    void reportAndNotificationDerivedReadsKeepLocksAndReporterFetching() {
        Member reporter = member("reporter", null);
        Report report = reports.saveAndFlush(Report.builder().reporter(reporter)
                .targetType(ReportTargetType.MEMBER).targetId("target")
                .reason(ReportReason.ETC).description("description").build());
        Notification notification = notifications.saveAndFlush(Notification.builder().member(reporter)
                .message("message").readStatus(Status.UNREAD)
                .notiGroup(spring.study.notification.entity.Group.ADMIN).build());
        entityManager.clear();

        Report loadedReport = reports.findForUpdateByIdAndReporterIsNotNull(report.getId()).orElseThrow();
        assertThat(Hibernate.isInitialized(loadedReport.getReporter())).isTrue();
        assertLocked(loadedReport);
        assertLocked(notifications.findForReadById(notification.getId()).orElseThrow());
    }

    private void assertLocked(Object entity) {
        assertThat(entityManager.getEntityManager().getLockMode(entity)).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    private Member member(String name, LocalDateTime lastLoginTime) {
        return members.saveAndFlush(Member.builder().email(name + "@example.test").pwd("unused")
                .name(name).role(Role.USER).phone(name).birth("2000-01-01").profile("profile.png")
                .lastLoginTime(lastLoginTime).build());
    }

    private Account account(String number, AccountType type) {
        return Account.builder().account(number).amount(0L).name(number).accountType(type).build();
    }
}
