package spring.study.staticjs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountInterestUiRegressionTest {
    private static final Path ACCOUNT_JS = Path.of("src/main/resources/static/js/account/page.js");
    private static final Path TRANSACTION_JS = Path.of("src/main/resources/static/js/account/transactions.js");

    @Test
    void accountPageShouldSupportInterestSettlementSelection() throws IOException {
        String accountJs = Files.readString(ACCOUNT_JS);

        assertTrue(accountJs.contains("checkingAccounts.length === 1"),
                "a sole checking account should be selected automatically");
        assertTrue(accountJs.contains("renderTerminationForm(accountId, checkingAccounts"),
                "multiple checking accounts should render a settlement selector");
        assertTrue(accountJs.contains("/terminate`"),
                "interest-bearing accounts should call the termination settlement API");
        assertTrue(accountJs.contains("account.estimatedInterest"),
                "the account page should show currently estimated interest");
    }

    @Test
    void transactionPageShouldLabelInterestAndTerminationRecords() throws IOException {
        String transactionJs = Files.readString(TRANSACTION_JS);

        assertTrue(transactionJs.contains("INTEREST: '이자'"));
        assertTrue(transactionJs.contains("TERMINATION: '해지 정산'"));
    }
}
