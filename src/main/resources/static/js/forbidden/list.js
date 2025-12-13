const word = document.getElementById('word');
const search = document.getElementById('search');
const btn = document.getElementById('btn');
const div = document.getElementById('uploadDiv')
const searchDiv = document.querySelector('.searchDiv')
const risk = document.getElementById('risk')

if (btn) {
    btn.addEventListener('click', (event) => {
        if (div.style.display === 'none')
            div.style.display = 'inline';
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
    search.addEventListener('keydown', (event) => {
        if (event.key == 'Enter') {
            fnBefore();

            fetch(`/api/forbidden/word/search?word=` + search.value, {
                method: 'GET',
                headers: {
                    "Content-Type": "application/json; charset=utf-8",
                },
                credentials: "include",
            })
            .then((response) => response.json())
            .then((json) => {
                if (json['result'] > 0) {
                    json.list.forEach(data => {
                        fnCreate(data)
                    })
                } else
                    alert("다시 시도하여주십시오")
            })
            .catch((error) => {
                console.error(error);
                alert("다시 시도하여주십시오")
            })
        }
    })
}

function fnFindProposal() {
    fnBefore();

    fetch(`/api/forbidden/word/proposal`, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['result'] > 0) {
            json.list.forEach(data => {
                fnCreate(data)
            })
        } else
            alert("다시 시도하여주십시오")
    })
    .catch((error) => {
        console.error(error)
        alert("다시 시도하여주십시오")
    })
}

function fnFindExamine() {
    fnBefore();

    fetch(`/api/forbidden/word/examine`, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['result'] > 0) {
            json.list.forEach(data => {
                fnCreate(data)
            })
        } else
            alert("다시 시도하여주십시오")
    })
    .catch((error) => {
        console.error(error)
        alert("다시 시도하여주십시오")
    })
}

function fnFindApproval() {
    fnBefore();

    fetch(`/api/forbidden/word/approval`, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['result'] > 0) {
            json.list.forEach(data => {
                fnCreate(data)
            })
        } else
            alert("다시 시도하여주십시오")
    })
    .catch((error) => {
        console.error(error)
        alert("다시 시도하여주십시오")
    })
}

function fnBefore() {
    const field = document.querySelector('.list')

    if (field)
        field.remove();

    area = document.createElement('div');
    area.className = 'list';
}

function fnCreate(data) {
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