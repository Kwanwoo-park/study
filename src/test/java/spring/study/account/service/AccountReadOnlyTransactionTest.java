package spring.study.account.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import spring.study.account.repository.AccountRepository;
import spring.study.notification.service.NotificationService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop", showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY, connection = EmbeddedDatabaseConnection.H2)
@Import(AccountService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AccountReadOnlyTransactionTest {
    @Autowired
    private AccountService service;

    @SpyBean
    private AccountRepository repository;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    private final List<String> accountNumbers = new ArrayList<>();

    @AfterEach
    void cleanup() {
        accountNumbers.forEach(service::deleteByAccount);
    }

    @Test
    void standaloneServiceQueryStartsReadOnlyTransaction() {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
            assertThat(entityManager.unwrap(Session.class).isDefaultReadOnly()).isTrue();
            return List.of();
        }).when(repository).findAll();

        assertThat(service.findAll()).isEmpty();
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
    }
}
