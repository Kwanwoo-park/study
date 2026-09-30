package spring.study.account.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import spring.study.account.dto.AccountResponseDto;
import spring.study.account.dto.AccountSettlementResult;
import spring.study.account.dto.AccountCreateRequestDto;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountStatus;
import spring.study.account.entity.AccountType;
import spring.study.account.repository.AccountRepository;
import spring.study.member.entity.Member;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;

    @Transactional
    public Account createAccount(Member member, AccountCreateRequestDto requestDto, Account source) {
        if (requestDto == null) requestDto = new AccountCreateRequestDto();
        AccountType resolvedType = requestDto.getAccountType() == null ? AccountType.DEPOSIT_WITHDRAWAL : requestDto.getAccountType();

        if (resolvedType.isInterestBearing() && !accountRepository.existsByMemberAndAccountTypeAndAccountStatus(member, AccountType.DEPOSIT_WITHDRAWAL, AccountStatus.ACTIVE)) {
            throw new IllegalArgumentException("예적금 계좌를 만들려면 먼저 활성 상태의 입출금 계좌가 필요합니다");
        }

        if (resolvedType == AccountType.INSTALLMENT_SAVINGS) {
            validateSavingsRequest(requestDto);
            validateOpeningSource(source, member, requestDto.getMonthlySavingsAmount(), "적금 납입");
        } else if (resolvedType == AccountType.TIME_DEPOSIT) {
            validateTimeDepositRequest(requestDto);
            validateOpeningSource(source, member, requestDto.getTimeDepositAmount(), "예금");
        }

        String accountFirst = "919";
        String accountLast = createAccountLast();
        String accountNumber = accountFirst + accountLast;

        while (accountRepository.existsById(accountNumber)) {
            accountLast = createAccountLast();
            accountNumber = accountFirst + accountLast;
        }

        Account account = Account.builder()
                .account(accountNumber)
                .amount(0L)
                .name(createDefaultAccountName(resolvedType))
                .accountType(resolvedType)
                .member(member)
                .build();

        if (resolvedType == AccountType.INSTALLMENT_SAVINGS)
            account.configureSavingsAutoTransfer(source, requestDto.getMonthlySavingsAmount(), requestDto.getMonthlySavingsDay(), LocalDate.now());
        else if (resolvedType == AccountType.TIME_DEPOSIT)
            account.configureTimeDepositTerm(requestDto.getMaturityMonths());

        Account savedAccount = accountRepository.save(account);

        if (resolvedType == AccountType.INSTALLMENT_SAVINGS)
            openSavingsAccount(source, savedAccount, requestDto.getMonthlySavingsAmount());
        else if (resolvedType == AccountType.TIME_DEPOSIT)
            openTimeDeposit(source, savedAccount, requestDto.getTimeDepositAmount());

        return savedAccount;
    }

    @Transactional(readOnly = true)
    public List<String> findDueSavingsAccountNumbers(LocalDate processingDate) {
        return accountRepository.findDueSavingsAccountNumbers(AccountType.INSTALLMENT_SAVINGS, List.of(AccountStatus.ACTIVE, AccountStatus.MATURED), processingDate);
    }

    @Transactional(readOnly = true)
    public Account findByAccount(String accountNum) {
        return accountRepository.findById(accountNum).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 계좌입니다"
        ));
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Account findByAccountForUpdate(String accountNum) {
        return accountRepository.findByAccountForUpdate(accountNum)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계좌입니다"));
    }

    @Transactional(readOnly = true)
    public List<AccountResponseDto> findByMember(Member member) {
        return accountRepository.findByMember(member).stream().map(AccountResponseDto::new).toList();
    }

    @Transactional(readOnly = true)
    public List<AccountResponseDto> findActiveByMember(Member member) {
        return accountRepository.findByMemberAndAccountStatus(member, AccountStatus.ACTIVE).stream().map(AccountResponseDto::new).toList();
    }

    @Transactional(readOnly = true)
    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    @Transactional(readOnly = true)
    public boolean existsByAccount(String accountNum) {
        return accountRepository.existsById(accountNum);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveSavingsUsingSource(Account sourceAccount) {
        return accountRepository.existsBySavingsSourceAccountAndAccountStatusIn(sourceAccount, List.of(AccountStatus.ACTIVE, AccountStatus.MATURED));
    }

    @Transactional
    public void changeAccountName(String accountNum, String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("계좌명을 입력해주세요");
        else if (name.trim().length() > 100)
            throw new IllegalArgumentException("계좌명은 100자 이하여야 합니다");

        Account account = findByAccount(accountNum);

        account.changeName(name.trim());
    }

    @Transactional
    public void tranAccount (Account account, Account tranAccount, Long amount, LocalDateTime transactionTime) {
        validateAmount(amount);
        validateOutgoingTransaction(account);
        validateIncomingTransaction(tranAccount);

        prepareBalanceChange(account, transactionTime);
        prepareBalanceChange(tranAccount, transactionTime);

        if (account.getAmount() - amount < 0)
            throw new IllegalArgumentException("이체 금액이 계좌 잔액보다 큽니다");

        account.subAmount(amount);
        tranAccount.addAmount(amount);
    }

    @Transactional
    public void deposit(Account account, Long amount, LocalDateTime transactionTime) {
        validateAmount(amount);
        validateIncomingTransaction(account);
        prepareBalanceChange(account, transactionTime);
        account.addAmount(amount);
    }

    @Transactional
    public AccountSettlementResult terminateInterestAccount(Account source, Account settlementAccount, Member member,
                                                            LocalDateTime time) {
        validateSettlement(source, settlementAccount, member);
        boolean matured = source.getMaturityAt() != null && !time.isBefore(source.getMaturityAt());
        long principal = source.getAmount();
        long interest = source.terminateAndGetInterest(time);
        long settlementAmount = Math.addExact(principal, interest);

        source.clearAmount();
        settlementAccount.addAmount(principal);

        if (interest > 0L) {
            settlementAccount.addAmount(interest);
        }

        return new AccountSettlementResult(source.getAccount(), settlementAccount.getAccount(), principal, interest, settlementAmount, matured);
    }

    @Transactional
    public void markMaturedAccounts(LocalDateTime now) {
        accountRepository.findByAccountStatusAndMaturityAtLessThanEqual(AccountStatus.ACTIVE, now)
                .forEach(account -> account.accrueInterestUntil(now));
    }

    public void accrueInterest(Account account, LocalDateTime calculationTime) {
        if (account != null) prepareBalanceChange(account, calculationTime);
    }

    public void validateOutgoingTransaction(Account account) {
        if (account.getAccountType() == AccountType.INSTALLMENT_SAVINGS) {
            throw new IllegalArgumentException("적금 계좌에서는 출금 또는 이체할 수 없습니다");
        }
        if (account.getAccountType() == AccountType.TIME_DEPOSIT) {
            throw new IllegalArgumentException("예금 계좌에서는 입금, 출금 또는 이체할 수 없습니다");
        }
    }

    @Transactional
    public void deleteByAccount(String accountNum) {
        accountRepository.deleteById(accountNum);
    }

    @Transactional
    public void deleteByMember(Member member) {
        accountRepository.deleteByMember(member);
    }

    private String createAccountLast() {
        long timestamp = Instant.now().toEpochMilli();
        int digitLength = ThreadLocalRandom.current().nextInt(10, 14);
        long min = (long) Math.pow(10, digitLength - 1);
        long max = (long) Math.pow(10, digitLength);
        long range = max - min;
        long randomValue = ThreadLocalRandom.current().nextLong(range);

        return String.valueOf(min + Math.floorMod(timestamp + randomValue, range));
    }

    private String createDefaultAccountName(AccountType accountType) {
        AccountType resolvedType = accountType == null ? AccountType.DEPOSIT_WITHDRAWAL : accountType;

        return resolvedType.getDefaultAccountName();
    }

    private void validateActive(Account account) {
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("만기 또는 해지된 계좌는 거래할 수 없습니다");
        }
    }

    private void validateIncomingTransaction(Account account) {
        if (account.getAccountType() == AccountType.TIME_DEPOSIT) {
            throw new IllegalArgumentException("예금 계좌에서는 입금, 출금 또는 이체할 수 없습니다");
        }
    }

    private void prepareBalanceChange(Account account, LocalDateTime calculationTime) {
        account.accrueInterestUntil(calculationTime);
        validateActive(account);
    }

    private void validateSettlement(Account source, Account settlementAccount, Member member) {
        if (source.getMember() == null || !source.getMember().getId().equals(member.getId()) || settlementAccount.getMember() == null || !settlementAccount.getMember().getId().equals(member.getId())) {
            throw new SecurityException("본인 계좌로만 해지 정산할 수 있습니다");
        } else if (!source.isInterestBearing()) {
            throw new IllegalArgumentException("예적금 계좌만 해지할 수 있습니다");
        } else if (source.getAccountStatus() == AccountStatus.TERMINATED) {
            throw new IllegalArgumentException("이미 해지된 계좌입니다");
        } else if (settlementAccount.getAccountType() != AccountType.DEPOSIT_WITHDRAWAL
                || settlementAccount.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("활성 상태의 입출금 계좌로만 정산할 수 있습니다");
        } else if (source.getAccount().equals(settlementAccount.getAccount())) {
            throw new IllegalArgumentException("해지할 계좌와 정산 계좌는 다르게 선택해주세요");
        }
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Account resolveSavingsSourceAccount(Member member, AccountCreateRequestDto requestDto) {
        validateSavingsRequest(requestDto);

        List<Account> checkingAccounts = accountRepository.findByMemberAndAccountTypeAndAccountStatus(member, AccountType.DEPOSIT_WITHDRAWAL, AccountStatus.ACTIVE);
        String requestedSource = requestDto.getSavingsSourceAccount();
        Account selected;

        if (checkingAccounts.size() == 1 && (requestedSource == null || requestedSource.isBlank())) {
            selected = checkingAccounts.get(0);
        } else {
            if (requestedSource == null || requestedSource.isBlank()) {
                throw new IllegalArgumentException("적금 자동이체에 사용할 입출금 계좌를 선택해주세요");
            }

            selected = checkingAccounts.stream()
                    .filter(account -> account.getAccount().equals(requestedSource))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "본인의 활성 입출금 계좌만 자동이체 계좌로 선택할 수 있습니다"
                    ));
        }

        return lockAndValidateSavingsSource(selected, member, requestDto.getMonthlySavingsAmount());
    }

    private Account lockAndValidateSavingsSource(Account selected, Member member, long paymentAmount) {
        Account lockedSource = accountRepository.findByAccountForUpdate(selected.getAccount())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계좌입니다"));

        if (lockedSource.getMember() == null || !lockedSource.getMember().getId().equals(member.getId()) || lockedSource.getAccountType() != AccountType.DEPOSIT_WITHDRAWAL || lockedSource.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("본인의 활성 입출금 계좌만 자동이체 계좌로 선택할 수 있습니다");
        } else if (lockedSource.getAmount() < paymentAmount) {
            throw new IllegalArgumentException("선택한 입출금 계좌의 잔액이 적금 납입 금액보다 부족합니다");
        }

        return lockedSource;
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Account resolveTimeDepositSourceAccount(Member member, AccountCreateRequestDto requestDto) {
        validateTimeDepositRequest(requestDto);

        List<Account> checkingAccounts = accountRepository.findByMemberAndAccountTypeAndAccountStatus(member, AccountType.DEPOSIT_WITHDRAWAL, AccountStatus.ACTIVE);
        String requestedSource = requestDto.getTimeDepositSourceAccount();
        Account selected;

        if (checkingAccounts.size() == 1 && (requestedSource == null || requestedSource.isBlank())) {
            selected = checkingAccounts.get(0);
        } else {
            if (requestedSource == null || requestedSource.isBlank()) {
                throw new IllegalArgumentException("예금 원금을 출금할 입출금 계좌를 선택해주세요");
            }

            selected = checkingAccounts.stream()
                    .filter(account -> account.getAccount().equals(requestedSource))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "본인의 활성 입출금 계좌만 예금 출금 계좌로 선택할 수 있습니다"
                    ));
        }

        Account lockedSource = accountRepository.findByAccountForUpdate(selected.getAccount())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계좌입니다"));

        if (lockedSource.getMember() == null || !lockedSource.getMember().getId().equals(member.getId()) || lockedSource.getAccountType() != AccountType.DEPOSIT_WITHDRAWAL || lockedSource.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("본인의 활성 입출금 계좌만 예금 출금 계좌로 선택할 수 있습니다");
        } else if (lockedSource.getAmount() < requestDto.getTimeDepositAmount()) {
            throw new IllegalArgumentException("선택한 입출금 계좌의 잔액이 예금 금액보다 부족합니다");
        }

        return lockedSource;
    }

    private void openTimeDeposit(Account source, Account timeDeposit, long amount) {
        source.subAmount(amount);
        timeDeposit.addAmount(amount);
    }

    private void openSavingsAccount(Account source, Account savings, long amount) {
        source.subAmount(amount);
        savings.addAmount(amount);

        savings.completeInitialSavingsPayment(LocalDateTime.now().toLocalDate());
    }
    private void validateSavingsRequest(AccountCreateRequestDto requestDto) {
        if (requestDto == null || requestDto.getMonthlySavingsAmount() == null || requestDto.getMonthlySavingsAmount() < 10_000L) {
            throw new IllegalArgumentException("적금 월 납입액은 1만원 이상이어야 합니다");
        } else if (requestDto.getMonthlySavingsDay() == null || requestDto.getMonthlySavingsDay() < 1 || requestDto.getMonthlySavingsDay() > 31) {
            throw new IllegalArgumentException("적금 자동이체일은 1일부터 31일 사이로 선택해주세요");
        } else if (!Boolean.TRUE.equals(requestDto.getAutoTerminationAcknowledged())) {
            throw new IllegalArgumentException("3일 내 미납 시 적금 계좌가 자동 해지된다는 내용에 동의해주세요");
        }

    }

    private void validateTimeDepositRequest(AccountCreateRequestDto requestDto) {
        if (requestDto == null || requestDto.getTimeDepositAmount() == null || requestDto.getTimeDepositAmount() < 10_000L) {
            throw new IllegalArgumentException("예금 금액은 1만원 이상이어야 합니다");
        } else if (requestDto.getMaturityMonths() == null || requestDto.getMaturityMonths() < 1 || requestDto.getMaturityMonths() > 24) {
            throw new IllegalArgumentException("예금 만기 기간은 1개월부터 24개월까지 선택할 수 있습니다");
        }

    }

    private void validateOpeningSource(Account source, Member member, long amount, String product) {
        if (source == null || source.getMember() == null || member == null || !source.getMember().getId().equals(member.getId())
                || source.getAccountType() != AccountType.DEPOSIT_WITHDRAWAL || source.getAccountStatus() != AccountStatus.ACTIVE)
            throw new IllegalArgumentException("본인의 활성 입출금 계좌만 출금 계좌로 선택할 수 있습니다");
        if (source.getAmount() < amount)
            throw new IllegalArgumentException("선택한 입출금 계좌의 잔액이 " + product + " 금액보다 부족합니다");
    }

    private void validateAmount(Long amount) {
        if (amount == null || amount < 10_000L) throw new IllegalArgumentException("거래 금액은 1만원 이상이어야 합니다");
    }
}
