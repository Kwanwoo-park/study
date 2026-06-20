(function () {
    const pageSize = 20;
    const amountFormatter = new Intl.NumberFormat('ko-KR');
    const dateFormatter = new Intl.DateTimeFormat('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
    });
    const page = document.querySelector('.account-transaction-page');
    const title = document.getElementById('transactionPageTitle');
    const description = document.getElementById('transactionPageDescription');
    const body = document.getElementById('transactionPageBody');

    if (!page || !body) {
        return;
    }

    const account = String(page.dataset.account || '').replace(/\D/g, '');
    let currentPage = 0;
    let isLoading = false;

    body.addEventListener('click', async (event) => {
        const button = event.target.closest('[data-page-action]');
        if (!button || isLoading) {
            return;
        }

        if (button.dataset.pageAction === 'previous') {
            await loadTransactions(Math.max(currentPage - 1, 0));
            return;
        }

        if (button.dataset.pageAction === 'next') {
            await loadTransactions(currentPage + 1);
        }
    });

    window.addEventListener('load', () => loadTransactions(0));

    async function loadTransactions(pageNumber) {
        if (!account) {
            body.innerHTML = '<div class="account-page-message">조회할 계좌 정보가 없습니다</div>';
            return;
        }

        isLoading = true;
        title.innerText = '거래 내역';
        description.innerText = `${formatAccountNumber(account)} 계좌의 입금 및 이체 내역`;
        body.innerHTML = '<div class="account-page-message">거래 내역을 불러오는 중입니다</div>';

        try {
            const response = await fetch(
                `/api/account/transactions?account=${encodeURIComponent(account)}&page=${encodeURIComponent(pageNumber)}`,
                {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json; charset=utf-8',
                    },
                    credentials: 'include',
                }
            );
            const json = await response.json();

            if (json.result < 0) {
                alert(json.message || '다시 시도해주세요');
                body.innerHTML = '<div class="account-page-message">거래 내역을 불러오지 못했습니다</div>';
                return;
            }

            currentPage = Number(json.page || 0);
            renderTransactions(json);
        } catch (error) {
            console.error(error);
            alert('거래 내역 조회 중 오류가 발생했습니다');
            body.innerHTML = '<div class="account-page-message">거래 내역을 불러오지 못했습니다</div>';
        } finally {
            isLoading = false;
        }
    }

    function renderTransactions(data) {
        const transactions = data.list || [];

        if (transactions.length === 0) {
            body.innerHTML = '<div class="account-page-message">거래 내역이 없습니다</div>';
            return;
        }

        const fragment = document.createDocumentFragment();
        const list = document.createElement('ul');
        list.className = 'account-page-list account-transaction-list';

        transactions.forEach((transaction) => {
            const item = document.createElement('li');
            item.className = 'account-page-item account-transaction-item';
            item.innerHTML = renderTransaction(transaction);
            list.append(item);
        });

        fragment.append(list);
        fragment.append(renderPagination(data));

        body.innerHTML = '';
        body.append(fragment);
    }

    function renderPagination(data) {
        const wrapper = document.createElement('div');
        wrapper.className = 'account-transaction-pagination';

        const pageText = document.createElement('span');
        pageText.className = 'account-page-number';
        pageText.innerText = `${Number(data.page || 0) + 1} / ${Math.max(Number(data.totalPages || 1), 1)} 페이지`;

        const previousButton = document.createElement('button');
        previousButton.type = 'button';
        previousButton.className = 'btn btn-outline-secondary';
        previousButton.dataset.pageAction = 'previous';
        previousButton.disabled = !data.hasPrevious;
        previousButton.innerText = '이전';

        const nextButton = document.createElement('button');
        nextButton.type = 'button';
        nextButton.className = 'btn btn-outline-primary';
        nextButton.dataset.pageAction = 'next';
        nextButton.disabled = !data.hasNext;
        nextButton.innerText = '다음';

        wrapper.append(previousButton, pageText, nextButton);
        return wrapper;
    }

    function renderTransaction(transaction) {
        const typeLabel = getTypeLabel(transaction.transactionType);
        const statusLabel = getStatusLabel(transaction.transactionStatus);
        const isDeposit = transaction.depositAccount === account;

        return `
            <div class="account-transaction-row">
                <div>
                    <div class="account-page-name">${escapeHtml(typeLabel)}</div>
                    <div class="account-page-number">${escapeHtml(formatTransactionTime(transaction.transactionTime))}</div>
                </div>
                <div class="account-transaction-accounts">
                    <div>출금: ${escapeHtml(formatOptionalAccount(transaction.withdrawalAccount))}</div>
                    <div>입금: ${escapeHtml(formatOptionalAccount(transaction.depositAccount))}</div>
                </div>
                <div class="account-transaction-summary ${isDeposit ? 'deposit' : 'withdrawal'}">
                    <div class="account-page-amount">${isDeposit ? '+' : '-'}${formatAmount(transaction.amount)}원</div>
                    <div class="account-page-number">${escapeHtml(statusLabel)}</div>
                </div>
            </div>
            ${transaction.memo ? `<div class="account-transaction-memo">${escapeHtml(transaction.memo)}</div>` : ''}`;
    }

    function getTypeLabel(type) {
        const labels = {
            TRANSFER: '이체',
            DEPOSIT: '입금',
            WITHDRAWAL: '출금',
            PAYMENT: '결제',
            REFUND: '환불',
            FEE: '수수료',
        };

        return labels[type] || type || '-';
    }

    function getStatusLabel(status) {
        const labels = {
            COMPLETED: '완료',
            PENDING: '대기',
            FAILED: '실패',
            CANCELED: '취소',
        };

        return labels[status] || status || '-';
    }

    function formatTransactionTime(value) {
        if (!value) {
            return '-';
        }

        return dateFormatter.format(new Date(value));
    }

    function formatAmount(value) {
        return amountFormatter.format(Number(value || 0));
    }

    function formatOptionalAccount(value) {
        return value ? formatAccountNumber(value) : '-';
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
})();
