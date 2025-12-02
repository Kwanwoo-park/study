const notiDiv = document.querySelector('.noti-div')

window.onload = function() {
    loadNotification();
}

async function loadNotification() {
    try {
        const res = await fetch(`/api/notification/load`, {
            method: 'GET',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            credentials: "include",
        });
        const data = await res.json();

        fnDraw(data);
    } catch (e) {
        console.error('로드 오류', e);
    }
}

async function fnClick(group) {
    const notiUl = document.querySelector('.noti-ul')
    notiUl.remove();

    try {
        const res = await fetch(`/api/notification/sort/` + group, {
            method: 'GET',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            credentials: "include",
        });
        const data = await res.json();

        fnDraw(data);
    } catch (e) {
        console.error('로드 오류', e);
    }
}

function fnDraw(data) {
    const notiUl = document.createElement('ul');
    notiUl.className = 'noti-ul';

    data.list.forEach(item => {
        const mainLi = document.createElement('li');
        mainLi.className = 'notification-card';

        const message = document.createElement('p');
        message.innerText = item.message;
        message.className = 'notification-message';

        mainLi.append(message);

        const div = document.createElement('div');
        div.className = 'notification-actions';

        const btn = document.createElement('button')
        btn.className = 'btn btn-outline-dark mark-as-read-button'
        btn.id = item.id;

        if (item.readStatus == 'READ') {
            btn.disabled = true;
            btn.innerText = '읽음';
        } else {
            btn.innerText = '읽음으로 표시';
            btn.onclick = function() {
                fnRead(item.id);
            }
        }

        div.append(btn);

        mainLi.append(div);

        notiUl.append(mainLi);
    })

    notiDiv.append(notiUl);
}

function fnRead(id) {
    const button = document.getElementById(id);

    fetch(`/api/notification/mark-as-read?id=` + id, {
        method: 'PATCH',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => {
        if (response.status == 200) {
            alert('알림이 읽음으로 표시되었습니다');
            button.innerText = '읽음';
            button.disabled = true;
        }
        else {
            alert('알림 상태를 변경하는 중 오류가 발생했습니다');
        }
    })
    .catch((error) => {
        console.error(error);
        alert("다시 시도하여주십시오");
    });
}

function fnAllRead() {
    fetch(`/api/notification/mark-all-as-read`, {
        method: 'PATCH',
        headers: {
            "Content-Type": "appllication/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => {
        console.log(response);
        if (response.status == 200) {
            alert('알림이 모두 읽음으로 표시되었습니다');
            window.location.reload();
        } else {
            alert('알림 상태를 변경하는 중 오류가 발생했습니다');
        }
    })
    .catch((error) => {
        console.error(error);
        alert("다시 시도하여주십시오");
    })
}