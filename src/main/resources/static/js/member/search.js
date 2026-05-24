const nameInput = document.getElementById('name');
const searchResult = document.getElementById('searchResult');

let searchTimer;

if (nameInput && searchResult) {
    renderMessage('검색어를 입력해주세요.');

    nameInput.addEventListener('input', () => {
        window.clearTimeout(searchTimer);
        searchTimer = window.setTimeout(searchMembers, 250);
    });

    nameInput.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            window.clearTimeout(searchTimer);
            searchMembers();
        }
    });
}

async function searchMembers() {
    const keyword = nameInput.value.trim();

    if (!keyword) {
        renderMessage('검색어를 입력해주세요.');
        return;
    }

    renderMessage('검색 중입니다.');

    try {
        const response = await fetch(`/api/member/search?name=${encodeURIComponent(keyword)}`, {
            method: 'GET',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            credentials: "include",
        });
        const json = await response.json();

        if (json.result < 0) {
            alert(json.message || '다시 시도해주세요');
            return;
        }

        renderResults(json.list || []);
    } catch (error) {
        console.error(error);
        alert('다시 시도해주세요');
    }
}

function renderResults(list) {
    searchResult.innerHTML = '';

    if (list.length === 0) {
        renderMessage('검색 결과가 없습니다.');
        return;
    }

    list.forEach((member) => {
        const card = document.createElement('a');
        card.className = 'member-result-card';
        card.href = `/member/search/detail?email=${encodeURIComponent(member.email)}`;

        const img = document.createElement('img');
        img.src = member.profile;
        img.alt = '';
        img.className = 'member-result-profile';

        const info = document.createElement('div');
        info.className = 'member-result-info';

        const memberName = document.createElement('span');
        memberName.className = 'member-result-name';
        memberName.innerText = member.name;

        const email = document.createElement('span');
        email.className = 'member-result-email';
        email.innerText = member.email;

        info.append(memberName);
        info.append(email);
        card.append(img);
        card.append(info);
        searchResult.append(card);
    });
}

function renderMessage(message) {
    searchResult.innerHTML = '';

    const messageElement = document.createElement('div');
    messageElement.className = 'member-search-message';
    messageElement.innerText = message;

    searchResult.append(messageElement);
}
