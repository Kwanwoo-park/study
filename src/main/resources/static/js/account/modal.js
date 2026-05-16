(function (global) {
    const amountFormatter = new Intl.NumberFormat('ko-KR');

    function initAccountModal() {
        const modal = document.getElementById('accountModal');
        const modalOverlay = document.getElementById('accountModalOverlay');
        const modalClose = document.getElementById('accountModalClose');
        const title = document.getElementById('accountListTitle');
        const modalBody = document.getElementById('accountModalBody');
        const accountCreateBtn = document.getElementById('accountCreateBtn');

        if (!modal || !modalBody || modal.dataset.initialized === 'true') {
            return;
        }

        modal.dataset.initialized = 'true';

        let modalOpen = false;
        let isLoading = false;
        const memberEmail = document.querySelector('.container')?.dataset.memberEmail;

        if (modalOverlay) {
            modalOverlay.addEventListener('click', () => closeAccountModal());
        }

        if (modalClose) {
            modalClose.addEventListener('click', () => closeAccountModal());
        }

        if (accountCreateBtn) {
            accountCreateBtn.addEventListener('click', createAccount);
        }

        modalBody.addEventListener('click', async (event) => {
            const actionTarget = event.target.closest('[data-action]');
            if (!actionTarget) return;

            const action = actionTarget.dataset.action;
            const account = actionTarget.dataset.account;

            if (action === 'transfer-toggle') {
                toggleTransferForm(account);
                return;
            }

            if (action === 'transfer-submit') {
                await transferAccount(account);
            }
        });

        window.addEventListener('popstate', async (event) => {
            const state = event.state;

            if (state && state.accountModal) {
                await openAccountModal(false);
                return;
            }

            if (modalOpen) {
                closeAccountModal(true);
            }
        });

        async function openAccountModal(push = true) {
            modal.classList.remove('hidden');
            document.body.classList.add('account-modal-open');
            modalOpen = true;

            await loadAccounts();

            const modalUrl = memberEmail
                ? `/member/detail?email=${encodeURIComponent(memberEmail)}&account=true`
                : window.location.pathname;

            if (push) {
                history.pushState({ accountModal: true }, '', modalUrl);
            } else {
                history.replaceState({ accountModal: true }, '', modalUrl);
            }
        }

        function closeAccountModal(fromPopState = false) {
            modal.classList.add('hidden');
            modalBody.innerHTML = '';
            document.body.classList.remove('account-modal-open');
            modalOpen = false;

            if (!fromPopState && history.state && history.state.accountModal) {
                history.back();
            }
        }

        async function loadAccounts() {
            if (isLoading) {
                return;
            }

            isLoading = true;
            modalBody.innerHTML = '<div class="account-modal-loading">계좌를 불러오는 중입니다</div>';

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
                    alert('다시 시도하여주십시오');
                    return;
                }

                renderAccounts(json);
            } catch (error) {
                console.error(error);
                alert('다시 시도하여주십시오');
            } finally {
                isLoading = false;
            }
        }

        async function createAccount() {
            if (isLoading) {
                return;
            }

            isLoading = true;
            accountCreateBtn.disabled = true;

            try {
                const response = await fetch('/api/account/create', {
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
            const accounts = data.list || [];

            if (accounts.length === 0) {
                modalBody.innerHTML = '<div class="account-modal-empty">생성된 계좌가 없습니다</div>';
                return;
            }

            title.innerText = data.name + "님의 계좌목록";

            const list = document.createElement('ul');
            list.className = 'account-modal-list';

            accounts.forEach((account) => {
                const accountId = escapeAttribute(account.account);
                const item = document.createElement('li');
                item.className = 'account-modal-item';
                item.innerHTML = `
                    <div class="account-modal-row">
                        <div>
                            <div class="account-modal-name">${escapeHtml(account.name || '계좌')}</div>
                            <div class="account-modal-number">${escapeHtml(formatAccountNumber(account.account))}</div>
                        </div>
                        <div class="account-modal-amount">${formatAmount(account.amount)}원</div>
                        <button type="button" class="btn btn-success account-transfer-toggle" data-action="transfer-toggle" data-account="${accountId}">계좌이체</button>
                    </div>
                    <div class="account-transfer-form hidden" id="transferForm${accountId}">
                        <input type="text" class="form-control account-transfer-input" id="transferAccount${accountId}" placeholder="받는 계좌번호">
                        <input type="number" class="form-control account-transfer-input" id="transferAmount${accountId}" min="1" placeholder="이체 금액">
                        <button type="button" class="btn btn-primary" data-action="transfer-submit" data-account="${accountId}">이체</button>
                    </div>
                `;
                list.append(item);
            });

            modalBody.innerHTML = '';
            modalBody.append(list);
        }

        function toggleTransferForm(account) {
            const targetForm = document.getElementById(`transferForm${account}`);
            if (!targetForm) return;

            modalBody.querySelectorAll('.account-transfer-form').forEach((form) => {
                if (form !== targetForm) {
                    form.classList.add('hidden');
                }
            });

            targetForm.classList.toggle('hidden');
        }

        async function transferAccount(account) {
            if (isLoading) {
                return;
            }

            const tranAccountInput = document.getElementById(`transferAccount${account}`);
            const amountInput = document.getElementById(`transferAmount${account}`);
            const tranAccount = String(tranAccountInput?.value || '').replace(/\D/g, '');
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

        function formatAmount(value) {
            return amountFormatter.format(Number(value || 0));
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

        global.openAccountModal = openAccountModal;
        global.closeAccountModal = closeAccountModal;
    }

    global.initAccountModal = initAccountModal;
})(window);
