window.onload = async function() {
    await loadUserStatus();
    await loadNewUser();
    await loadNewBoard();
    await loadActiveChatting();
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
        credentials: "include",
    });
    const data = await res.json();

    if (data.count >= 0)
        fnNewUserDraw(data);
}

async function loadNewBoard() {
    const res = await fetch(`/api/admin/board/new`, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    });
    const data = await res.json();

    if (data.count >= 0)
        fnNewBoardDraw(data);
}

async function loadActiveChatting() {
    const res = await fetch(`/api/admin/chat/active`, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    });

    const data = await res.json();

    if (data.count >= 0)
        fnChattingActive(data);
}

function fnDraw(data) {
    const userDiv = document.getElementById('user-status');

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
    const newUserDiv = document.getElementById('new-user');

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

function fnNewBoardDraw(data) {
    const newBoardDiv = document.getElementById('new-board');

    const boardCount = document.createElement('label');
    boardCount.innerText = "신규 게시글 수: " + data.count;

    newBoardDiv.append(boardCount);

    const ul = document.createElement('ul');

    data.list.forEach(board => {
        const li = document.createElement('li');
        li.onclick = function() {
            fnBoardMove(board.id);
        }

        const boardId = document.createElement('label');
        boardId.innerText = board.id;

        const boardUser = document.createElement('label');
        boardUser.innerText = "(작성자: " + board.member.name + ")";
        boardUser.style.color = 'gray';

        li.append(boardId);
        li.append(boardUser);

        ul.append(li);
    })

    newBoardDiv.append(ul);
}

function fnChattingActive(data) {
    const activeChattingDiv = document.getElementById('active-chat-room');

    const roomCount = document.createElement('label');
    roomCount.innerText = "활성화된 채팅방 수: " + data.count;

    activeChattingDiv.append(roomCount);

    const ul = document.createElement('ul');

    data.list.forEach(room => {
        const li = document.createElement('li');
        li.onclick = function() {
            fnChatRoomMove(room.roomId);
        }

        const roomId = document.createElement('label');
        roomId.innerText = room.id;

        const lastMessage = document.createElement('label');
        lastMessage.innerText = "(마지막 메시지: " + room.lastMessage + ")";
        lastMessage.style.color = 'gray';

        li.append(roomId);
        li.append(lastMessage);

        ul.append(li);
    })

    activeChattingDiv.append(ul);
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

function fnBoardMove(id) {
    location.replace(`/board/view?id=` + id);
}

function fnChatRoomMove(id) {
    location.replace(`/chat/chatRoom?roomId=` + id);
}