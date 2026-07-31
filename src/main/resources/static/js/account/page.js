(function () {
    const amountFormatter = new Intl.NumberFormat('ko-KR');
    const page = document.querySelector('.account-page');
    const title = document.getElementById('accountPageTitle');
    const description = document.getElementById('accountPageDescription');
    const body = document.getElementById('accountPageBody');
    const accountCreateControls = document.getElementById('accountCreateControls');
    const accountCreateBtn = document.getElementById('accountCreateBtn');
    const accountTypeSelect = document.getElementById('accountTypeSelect');

    if (!page || !body) {
        return;
    }

    const transferTarget = {
        account: String(page.dataset.tranAccount || '').replace(/\D/g, ''),
        name: page.dataset.tranName || '회원',
    };
    const isTransferPage = Boolean(transferTarget.account);
    let isLoading = false;
    let loadedAccounts = [];

    if (accountCreateBtn) {
        accountCreateControls?.classList.toggle('hidden', isTransferPage);
        accountCreateBtn.addEventListener('click', createAccount);
    }

    body.addEventListener('click', async (event) => {
        const actionTarget = event.target.closest('[data-action]');
        if (!actionTarget) return;

        const action = actionTarget.dataset.action;
        const account = actionTarget.dataset.account;

        if (action === 'transfer-toggle') {
            toggleForm(`transferForm${account}`);
            return;
        }

        if (action === 'deposit-toggle') {
            toggleForm(`depositForm${account}`);
            return;
        }

        if (action === 'transactions') {
            window.location.href = `/account/transactions?account=${encodeURIComponent(account)}`;
            return;
        }

        if (action === 'transfer-submit') {
            await transferAccount(account, actionTarget.dataset.tranAccount);
            return;
        }

        if (action === 'deposit-submit') {
            await depositAccount(account);
            return;
        }

        if (action === 'terminate-toggle') {
            const settlementAccount = actionTarget.dataset.settlementAccount;
            if (settlementAccount) {
                await terminateAccount(account, settlementAccount);
            } else {
                toggleForm(`terminationForm${account}`);
            }
            return;
        }

        if (action === 'terminate-submit') {
            const settlementSelect = document.getElementById(`settlementAccount${account}`);
            await terminateAccount(account, settlementSelect?.value);
        }
    });

    window.addEventListener('load', loadAccounts);

    async function loadAccounts() {
        if (isLoading) {
            return;
        }

        isLoading = true;
        body.innerHTML = '<div class="account-page-message">계좌를 불러오는 중입니다</div>';

        try {
            const response = await fetch('/api/account/list', {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json; charset=utf-8',
                },
                credentials: 'include',
            });
            const json = await response.json();

            if (json.result < 0) {
                alert(json.message || '다시 시도해주세요');
                return;
            }

            renderAccounts(json);
        } catch (error) {
            console.error(error);
            alert('다시 시도해주세요');
        } finally {
            isLoading = false;
        }
    }

    async function createAccount() {
        if (isLoading || !accountCreateBtn) {
            return;
        }

        isLoading = true;
        accountCreateBtn.disabled = true;

        try {
            const accountType = accountTypeSelect?.value || 'DEPOSIT_WITHDRAWAL';
            const requiresCheckingAccount = accountType === 'INSTALLMENT_SAVINGS'
                || accountType === 'TIME_DEPOSIT';
            const hasCheckingAccount = loadedAccounts.some((account) =>
                account.accountType === 'DEPOSIT_WITHDRAWAL'
                && normalizeAccountStatus(account) === 'ACTIVE'
            );
            if (requiresCheckingAccount && !hasCheckingAccount) {
                alert('예적금 계좌를 만들려면 먼저 입출금 계좌를 생성해주세요');
                return;
            }

            const response = await fetch(`/api/account/create?accountType=${encodeURIComponent(accountType)}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json; charset=utf-8',
                },
                credentials: 'include',
            });
            const json = await response.json();

            if (json.result < 0) {
                alert(json.message || '계좌 생성에 실패했습니다');
                return;
            }

            isLoading = false;
            await loadAccounts();
        } catch (error) {
            console.error(error);
            alert('계좌 생성에 실패했습니다');
        } finally {
            isLoading = false;
            accountCreateBtn.disabled = false;
        }
    }

    function renderAccounts(data) {
        loadedAccounts = data.list || [];
        const accounts = (data.list || []).filter((account) =>
            !isTransferPage || normalizeAccountStatus(account) === 'ACTIVE'
        );

        title.innerText = isTransferPage
            ? `${transferTarget.name}님에게 이체`
            : `${data.name}님의 계좌목록`;
        description.innerText = isTransferPage
            ? `받는 계좌: ${formatAccountNumber(transferTarget.account)}`
            : '계좌를 생성하고 잔액을 확인할 수 있습니다';

        if (accounts.length === 0) {
            body.innerHTML = '<div class="account-page-message">생성된 계좌가 없습니다</div>';
            return;
        }

        const list = document.createElement('ul');
        list.className = 'account-page-list';
        const checkingAccounts = accounts.filter((account) =>
            account.accountType === 'DEPOSIT_WITHDRAWAL'
            && normalizeAccountStatus(account) === 'ACTIVE'
        );

        accounts.forEach((account) => {
            const accountId = escapeAttribute(account.account);
            const canTransfer = normalizeAccountStatus(account) === 'ACTIVE'
                && Number(account.amount || 0) >= 10000;
            const item = document.createElement('li');
            item.className = 'account-page-item';
            item.innerHTML = isTransferPage
                ? renderTransferTargetAccount(account, accountId)
                : renderOwnAccount(account, accountId, canTransfer, checkingAccounts);
            list.append(item);
        });

        body.innerHTML = '';
        body.append(list);
    }

    function renderTransferTargetAccount(account, accountId) {
        return `
            <div class="account-page-row">
                <div>
                    <div class="account-page-name">${escapeHtml(account.name || '계좌')}</div>
                    <div class="account-type-badge">${escapeHtml(getAccountTypeName(account))}</div>
                    <div class="account-page-number">${escapeHtml(formatAccountNumber(account.account))}</div>
                </div>
                <div class="account-page-amount">${formatAmount(account.amount)}원</div>
                <div class="account-action-group">
                    <button type="button" class="btn btn-outline-primary" data-action="deposit-toggle" data-account="${accountId}">입금</button>
                    <button type="button" class="btn btn-success" data-action="transfer-toggle" data-account="${accountId}">선택</button>
                </div>
            </div>
            <div class="account-transfer-form hidden" id="depositForm${accountId}">
                <input type="number" class="form-control account-transfer-input" id="depositAmount${accountId}" min="10000" step="10000" placeholder="입금 금액">
                <button type="button" class="btn btn-primary" data-action="deposit-submit" data-account="${accountId}">입금</button>
            </div>
            <div class="account-transfer-form hidden" id="transferForm${accountId}">
                <input type="number" class="form-control account-transfer-input" id="transferAmount${accountId}" min="10000" step="10000" placeholder="이체 금액">
                <button type="button" class="btn btn-primary" data-action="transfer-submit" data-account="${accountId}" data-tran-account="${escapeAttribute(transferTarget.account)}">이체</button>
            </div>`;
    }

    function renderOwnAccount(account, accountId, canTransfer, checkingAccounts) {
        const accountStatus = normalizeAccountStatus(account);
        const isActive = accountStatus === 'ACTIVE';
        const isInterestAccount = account.accountType === 'INSTALLMENT_SAVINGS'
            || account.accountType === 'TIME_DEPOSIT';
        const canTerminate = isInterestAccount && accountStatus !== 'TERMINATED';

        return `
            <div class="account-page-row">
                <div>
                    <div class="account-page-name">${escapeHtml(account.name || '계좌')}</div>
                    <div class="account-type-badge">${escapeHtml(getAccountTypeName(account))}</div>
                    <div class="account-status-badge ${accountStatus.toLowerCase()}">${escapeHtml(getAccountStatusName(accountStatus))}</div>
                    <div class="account-page-number">${escapeHtml(formatAccountNumber(account.account))}</div>
                    ${renderInterestDetails(account)}
                </div>
                <div class="account-page-amount">${formatAmount(account.amount)}원</div>
                <div class="account-action-group">
                    <button type="button" class="btn btn-outline-secondary" data-action="transactions" data-account="${accountId}">거래 내역</button>
                    ${isActive ? `<button type="button" class="btn btn-outline-primary" data-action="deposit-toggle" data-account="${accountId}">입금</button>` : ''}
                    ${canTransfer ? `<button type="button" class="btn btn-success" data-action="transfer-toggle" data-account="${accountId}">계좌이체</button>` : ''}
                    ${canTerminate ? renderTerminationButton(accountId, checkingAccounts, accountStatus) : ''}
                </div>
            </div>
            ${isActive ? `<div class="account-transfer-form hidden" id="depositForm${accountId}">
                <input type="number" class="form-control account-transfer-input" id="depositAmount${accountId}" min="10000" step="10000" placeholder="입금 금액">
                <button type="button" class="btn btn-primary" data-action="deposit-submit" data-account="${accountId}">입금</button>
            </div>` : ''}
            ${canTransfer ? `<div class="account-transfer-form hidden" id="transferForm${accountId}">
                <input type="text" class="form-control account-transfer-input" id="transferAccount${accountId}" placeholder="받는 계좌번호">
                <input type="number" class="form-control account-transfer-input" id="transferAmount${accountId}" min="10000" step="10000" placeholder="이체 금액">
                <button type="button" class="btn btn-primary" data-action="transfer-submit" data-account="${accountId}">이체</button>
            </div>` : ''}
            ${canTerminate && checkingAccounts.length > 1 ? renderTerminationForm(accountId, checkingAccounts, accountStatus) : ''}`;
    }

    function renderTerminationButton(accountId, checkingAccounts, accountStatus) {
        if (checkingAccounts.length === 0) {
            return '<button type="button" class="btn btn-outline-danger" disabled title="정산받을 입출금 계좌가 필요합니다">입출금 계좌 필요</button>';
        }

        const settlementAccount = checkingAccounts.length === 1
            ? ` data-settlement-account="${escapeAttribute(checkingAccounts[0].account)}"`
            : '';
        const label = accountStatus === 'MATURED' ? '만기 해지' : '중도 해지';

        return `<button type="button" class="btn btn-outline-danger" data-action="terminate-toggle" data-account="${accountId}"${settlementAccount}>${label}</button>`;
    }

    function renderTerminationForm(accountId, checkingAccounts, accountStatus) {
        const options = checkingAccounts.map((checkingAccount) => `
            <option value="${escapeAttribute(checkingAccount.account)}">
                ${escapeHtml(checkingAccount.name)} (${escapeHtml(formatAccountNumber(checkingAccount.account))})
            </option>`).join('');
        const label = accountStatus === 'MATURED' ? '만기 해지' : '중도 해지';

        return `<div class="account-transfer-form hidden" id="terminationForm${accountId}">
            <select class="form-control" id="settlementAccount${accountId}" aria-label="정산받을 입출금 계좌">${options}</select>
            <button type="button" class="btn btn-danger" data-action="terminate-submit" data-account="${accountId}">${label}</button>
        </div>`;
    }

    function renderInterestDetails(account) {
        if (account.accountType === 'DEPOSIT_WITHDRAWAL') return '';

        const rate = Number(account.annualInterestRatePercent || 0);
        const maturity = account.maturityAt ? formatDate(account.maturityAt) : '-';
        const interestLabel = normalizeAccountStatus(account) === 'TERMINATED' ? '지급 이자' : '예상 이자';

        return `<div class="account-interest-info">
            <span>연 ${escapeHtml(rate)}%</span>
            <span>만기 ${escapeHtml(maturity)}</span>
            <span>${interestLabel} ${formatAmount(account.estimatedInterest)}원</span>
        </div>`;
    }

    function toggleForm(formId) {
        const targetForm = document.getElementById(formId);
        if (!targetForm) return;

        body.querySelectorAll('.account-transfer-form').forEach((form) => {
            if (form !== targetForm) {
                form.classList.add('hidden');
            }
        });

        targetForm.classList.toggle('hidden');
    }

    async function transferAccount(account, targetAccount) {
        if (isLoading) {
            return;
        }

        const tranAccountInput = document.getElementById(`transferAccount${account}`);
        const amountInput = document.getElementById(`transferAmount${account}`);
        const tranAccount = String(targetAccount || tranAccountInput?.value || '').replace(/\D/g, '');
        const amount = Number(amountInput?.value || 0);

        if (!tranAccount) {
            alert('받는 계좌번호를 입력해주세요');
            return;
        }

        if (!amount || amount < 1) {
            alert('이체 금액을 입력해주세요');
            return;
        }

        isLoading = true;

        try {
            const response = await fetch('/api/account/tran', {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json; charset=utf-8',
                },
                body: JSON.stringify({
                    account: account,
                    tranAccount: tranAccount,
                    amount: amount,
                }),
                credentials: 'include',
            });
            const json = await response.json();

            alert(json.message || '거래 결과 메시지가 없습니다');

            if (json.result > 0) {
                isLoading = false;
                await loadAccounts();
            }
        } catch (error) {
            console.error(error);
            alert('계좌이체 중 오류가 발생했습니다');
        } finally {
            isLoading = false;
        }
    }

    async function depositAccount(account) {
        if (isLoading) {
            return;
        }

        const amountInput = document.getElementById(`depositAmount${account}`);
        const amount = Number(amountInput?.value || 0);

        if (!amount || amount < 1) {
            alert('입금 금액을 입력해주세요');
            return;
        }

        isLoading = true;

        try {
            const response = await fetch('/api/account/deposit', {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json; charset=utf-8',
                },
                body: JSON.stringify({
                    account: account,
                    amount: amount,
                }),
                credentials: 'include',
            });
            const json = await response.json();

            alert(json.message || '입금 결과 메시지가 없습니다');

            if (json.result > 0) {
                isLoading = false;
                await loadAccounts();
            }
        } catch (error) {
            console.error(error);
            alert('입금 중 오류가 발생했습니다');
        } finally {
            isLoading = false;
        }
    }

    async function terminateAccount(account, settlementAccount) {
        if (isLoading || !settlementAccount) return;

        if (!window.confirm('원금과 해지일까지의 이자를 선택한 입출금 계좌로 정산할까요?')) {
            return;
        }

        isLoading = true;
        try {
            const response = await fetch(`/api/account/${encodeURIComponent(account)}/terminate`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json; charset=utf-8',
                },
                body: JSON.stringify({ settlementAccount }),
                credentials: 'include',
            });
            const json = await response.json();
            if (!response.ok || json.result < 0) {
                alert(json.message || '계좌 해지에 실패했습니다');
                return;
            }

            const settlement = json.settlement;
            alert(`${json.message}\n원금 ${formatAmount(settlement.principal)}원 + 이자 ${formatAmount(settlement.interest)}원`);
            isLoading = false;
            await loadAccounts();
        } catch (error) {
            console.error(error);
            alert('계좌 해지 중 오류가 발생했습니다');
        } finally {
            isLoading = false;
        }
    }

    function formatAmount(value) {
        return amountFormatter.format(Number(value || 0));
    }

    function getAccountTypeName(account) {
        if (account.accountTypeName) return account.accountTypeName;

        const names = {
            DEPOSIT_WITHDRAWAL: '입출금',
            INSTALLMENT_SAVINGS: '적금',
            TIME_DEPOSIT: '예금',
        };

        return names[account.accountType] || '입출금';
    }

    function normalizeAccountStatus(account) {
        return account.accountStatus || 'ACTIVE';
    }

    function getAccountStatusName(status) {
        const names = {
            ACTIVE: '정상',
            MATURED: '만기',
            TERMINATED: '해지',
        };

        return names[status] || status;
    }

    function formatDate(value) {
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return '-';

        return new Intl.DateTimeFormat('ko-KR', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
        }).format(date);
    }

    function formatAccountNumber(value) {
        const accountNumber = String(value || '').replace(/\D/g, '');

        if (accountNumber.length <= 3) {
            return accountNumber;
        }

        if (accountNumber.length <= 8) {
            return `${accountNumber.slice(0, 3)}-${accountNumber.slice(3)}`;
        }

        return `${accountNumber.slice(0, 3)}-${accountNumber.slice(3, 8)}-${accountNumber.slice(8)}`;
    }

    function escapeHtml(value) {
        return String(value)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    function escapeAttribute(value) {
        return String(value).replaceAll('"', '&quot;');
    }
})();
