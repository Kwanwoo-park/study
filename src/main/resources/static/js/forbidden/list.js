const word = document.getElementById('word');
const search = document.getElementById('search');
const btn = document.getElementById('btn');
const div = document.getElementById('uploadDiv')
const searchDiv = document.querySelector('.searchDiv')
const risk = document.getElementById('risk')
let searchTimer;

if (btn) {
    btn.addEventListener('click', (event) => {
        if (getComputedStyle(div).display === 'none')
            div.style.display = 'block';
        else {
            fetch(`/api/forbidden/word/apply`, {
                method: 'POST',
                headers: {
                    "Content-Type": "application/json; charset=utf-8",
                },
                body: JSON.stringify({
                    word: word.value,
                    risk: risk.value
                }),
                credentials: "include",
            })
            .then((response) => response.json())
            .then((json) => {
                if (json['result'] == -1)
                    alert("이미 신청되었거나 존재하는 단어입니다");
                else if (json['result'] < 0)
                    alert("다시 시도하여 주십시오");
                else
                    window.location.reload();
            })
            .catch((error) => {
                alert("다시 시도하여 주십시오");
            })
        }
    })
}

if (search) {
    renderMessage('검색어를 입력해주세요.');

    search.addEventListener('input', () => {
        window.clearTimeout(searchTimer);
        searchTimer = window.setTimeout(searchForbiddenWords, 250);
    });

    search.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            window.clearTimeout(searchTimer);
            searchForbiddenWords();
        }
    })
}

async function searchForbiddenWords() {
    const keyword = search.value.trim();

    if (!keyword) {
        renderMessage('검색어를 입력해주세요.');
        return;
    }

    renderMessage('검색 중입니다.');

    try {
        const response = await fetch(`/api/forbidden/word/search?word=${encodeURIComponent(keyword)}`, {
            method: 'GET',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            credentials: "include",
        });
        const json = await response.json();

        if (json.result < 0) {
            alert(json.message || '다시 시도하여주십시오');
            return;
        }

        renderResults(json.list || []);
    } catch (error) {
        console.error(error);
        alert("다시 시도하여주십시오")
    }
}

function fnFindProposal() {
    clearResults();

    fetch(`/api/forbidden/word/proposal`, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['result'] >= 0) {
            renderResults(json.list || []);
        } else
            alert("다시 시도하여주십시오")
    })
    .catch((error) => {
        console.error(error)
        alert("다시 시도하여주십시오")
    })
}

function fnFindExamine() {
    clearResults();

    fetch(`/api/forbidden/word/examine`, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['result'] >= 0) {
            renderResults(json.list || []);
        } else
            alert("다시 시도하여주십시오")
    })
    .catch((error) => {
        console.error(error)
        alert("다시 시도하여주십시오")
    })
}

function fnFindApproval() {
    clearResults();

    fetch(`/api/forbidden/word/approval`, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['result'] >= 0) {
            renderResults(json.list || []);
        } else
            alert("다시 시도하여주십시오")
    })
    .catch((error) => {
        console.error(error)
        alert("다시 시도하여주십시오")
    })
}

function clearResults() {
    searchDiv.innerHTML = '';
}

function renderResults(list) {
    clearResults();

    if (list.length === 0) {
        renderMessage('검색 결과가 없습니다.');
        return;
    }

    list.forEach(data => {
        fnCreate(data)
    })
}

function fnCreate(data) {
    const area = document.createElement('div');
    area.className = 'list';

    let text = document.createElement('div')
    text.className = 'text';

    let wordLabel = document.createElement('label')
    wordLabel.className = 'form-label';
    wordLabel.innerText = "Word: ";

    let wordText = document.createElement('label');
    wordText.className = 'form-label';
    wordText.innerText = data.word;

    text.append(wordLabel);
    text.append(wordText);

    let annotation = document.createElement('div')
    annotation.className = 'annotation';

    let statusLabel = document.createElement('label')
    statusLabel.innerText = "Status: ";

    let statusText = document.createElement('label');
    statusText.innerText = data.status;

    let riskLabel = document.createElement('label')
    riskLabel.innerText = "Risk: ";

    let riskText = document.createElement('label');
    riskText.innerText = data.risk;

    annotation.append(statusLabel);
    annotation.append(statusText);
    annotation.append(riskLabel);
    annotation.append(riskText);

    area.append(text);
    area.append(annotation);

    searchDiv.append(area)
}

function renderMessage(message) {
    clearResults();

    const messageElement = document.createElement('div');
    messageElement.className = 'forbidden-search-message';
    messageElement.innerText = message;

    searchDiv.append(messageElement);
}
