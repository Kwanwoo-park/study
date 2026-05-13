window.onload = async function() {
    await loadUserStatus();
    await loadNewUser();
}

async function loadUserStatus() {
    const res = await fetch(`/api/admin/member/online`, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    });
    const data = await res.json();

    if (data.count >= 0)
        fnDraw(data);
}

async function loadNewUser() {
    const res = await fetch(`/api/admin/member/new`, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "includes",
    });
    const data = await res.json();

    if (data.count >= 0)
        fnNewUserDraw(data);
}

function fnDraw(data) {
    const userDiv = document.querySelector('.user-status');

    const userCount = document.createElement('label');
    userCount.innerText = "현재 접속자 수: " + data.count;

    userDiv.append(userCount);

    const ul = document.createElement('ul');

    data.list.forEach(member => {
        const li = document.createElement('li');

        const userName = document.createElement('label');
        userName.innerText = member.name;

        const email = document.createElement('label');
        email.innerText = "(" + member.email + ")";
        email.style.color = 'gray';

        li.append(userName);
        li.append(email);

        ul.append(li);
    })

    userDiv.append(ul);
}

function fnNewUserDraw(data) {
    const newUserDiv = document.querySelector('.new-user');

    const userCount = document.createElement('label');
    userCount.innerText = "신규 가입자 수: " + data.count;

    newUserDiv.append(userCount);

    const ul = document.createElement('ul');

    data.list.forEach(member => {
        const li = document.createElement('li');

        const userName = document.createElement('label');
        userName.innerText = member.name;

        const email = document.createElement('label');
        email.innerText = "(" + member.email + ")";
        email.style.color = 'gray';

        li.append(userName);
        li.append(email);

        ul.append(li);
    });

    newUserDiv.append(ul);
}

function fnLogout() {
    fetch(`/api/member/logout`, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['result'] > 0) {
            location.replace(`/member/login`);
        } else
            alert("다시 시도하여주십시오");
    })
}