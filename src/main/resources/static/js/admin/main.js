window.onload = function() {
    loadUserStatus();
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

    fnDraw(data);
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

        const userName2 = document.createElement('label');
        userName2.innerText = "(" + member.email + ")";
        userName2.style.color = 'gray';

        li.append(userName);
        li.append(userName2);

        ul.append(li);
    })

    userDiv.append(ul);
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
        if (json == 1) {
            location.replace(`/member/login`);
        }
    })
}