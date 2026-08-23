package spring.study.staticjs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountInterestUiRegressionTest {
    private static final Path ACCOUNT_JS = Path.of("src/main/resources/static/js/account/page.js");
    private static final Path TRANSACTION_JS = Path.of("src/main/resources/static/js/account/transactions.js");
    private static final Path ACCOUNT_CSS = Path.of("src/main/resources/static/css/account/page.css");

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
        assertTrue(accountJs.contains("autoTerminationAcknowledged"),
                "savings creation should acknowledge automatic termination");
        assertTrue(accountJs.contains("payload.savingsSourceAccount"),
                "savings creation should send the selected checking account");
        assertTrue(accountJs.contains("monthlySavingsDay.value = String(new Date().getDate())"),
                "savings creation should default the payment day to today's day of month");
        assertTrue(accountJs.contains("!confirmCheckingAccountCreation()"),
                "checking account creation should require confirmation before sending the request");
        assertTrue(accountJs.contains("생성 직후 잔액은 0원이며"),
                "checking account confirmation should explain the initial balance and intended uses");
        assertTrue(accountJs.contains("payload.timeDepositAmount"),
                "time deposit creation should send the opening principal");
        assertTrue(accountJs.contains("payload.maturityMonths"),
                "time deposit creation should send a term of at most 24 months");
        assertTrue(accountJs.contains("현재 잔액"),
                "the selected checking account balance should be shown");
        assertTrue(accountJs.contains("account.outgoingTransferAllowed === true"),
                "interest accounts should not be offered as transfer withdrawal accounts");
        assertTrue(accountJs.contains("const canDeposit = isActive && account.depositAllowed === true"),
                "time deposit accounts should hide the regular deposit action");
    }

    @Test
    void transactionPageShouldLabelInterestAndTerminationRecords() throws IOException {
        String transactionJs = Files.readString(TRANSACTION_JS);

        assertTrue(transactionJs.contains("INTEREST: '이자'"));
        assertTrue(transactionJs.contains("TERMINATION: '해지 정산'"));
        assertTrue(transactionJs.contains("SAVINGS_PAYMENT: '적금 자동이체'"));
        assertTrue(transactionJs.contains("TIME_DEPOSIT_OPENING: '예금 개설 입금'"));
    }

    @Test
    void accountTypeAndStatusBadgesShouldSupportDarkMode() throws IOException {
        String accountCss = Files.readString(ACCOUNT_CSS);

        assertTrue(accountCss.contains("body.dark-mode .account-page .account-type-badge"));
        assertTrue(accountCss.contains("body.dark-mode .account-page .account-status-badge.matured"));
        assertTrue(accountCss.contains("body.dark-mode .account-page .account-status-badge.terminated"));
        assertTrue(accountCss.contains("body.dark-mode .account-page .account-product-create-options .form-control:focus"));
        assertTrue(accountCss.contains("body.dark-mode .account-page .account-savings-policy"));
    }
}
