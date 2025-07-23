const url = new URL(window.location.href)
const urlParams = url.searchParams

let socket = new WebSocket("wss://www.kwanwoo.site/ws/chat?roomId=" + urlParams.get('roomId'));
//let socket = new WebSocket("ws://localhost:8080/ws/chat?roomId=" + urlParams.get('roomId'));

const roomId = document.querySelector("#room").value;
const email = document.querySelector("#email").value;
const flag = document.querySelector("#flag").value;

const upload = document.getElementById("upload");
const btn = document.getElementById("btn");

let container = document.querySelector(".container");

if (btn)
    btn.addEventListener('click', () => upload.click());

window.onload = function() {
    container.scrollTop = container.scrollHeight;
}

socket.onopen = function(e) {
    console.log('open server!')

    if (flag == 'true')
        enterRoom(socket);
}

socket.onclose = function(e) {
    console.log('disconnect');
}

socket.onerror = function(e) {
    console.log(e);
}

socket.onmessage = function(e) {
    const json = JSON.parse(e.data);
    let msgArea = document.querySelector('.list-group-flush');

    let newMsgLi = document.createElement('li');
    let newMsgArea = document.createElement('span');
    let name = document.createElement('span');
    let newMsg = document.createElement('pre');
    let profile = document.createElement('img');
    let imgTalk = document.createElement('img');

    newMsgLi.className = "list-group-item";

    profile.src = json.member.profile;
    profile.className = "profile";

    name.innerText = json.member.name;
    name.className = 'chatname';

    if (json.member.email == email) {
        profile.align = 'right';
        imgTalk.align = 'right';

        newMsgArea.className = 'right';

        newMsgArea.append(name);
        newMsgArea.append(profile);
    }
    else {
        profile.align = 'left';
        imgTalk.align = 'left';

        newMsgArea.className = 'left';

        newMsgArea.append(profile);
        newMsgArea.append(name);
    }

    if (json.type == "IMAGE") {
        imgTalk.src = json.message;
        imgTalk.className = "chatimg";

        newMsgArea.append(imgTalk);
    }
    else {
        newMsg.innerText = json.message;
        newMsgArea.append(newMsg);
    }

    newMsgLi.append(newMsgArea);

    msgArea.append(newMsgLi);

    container.scrollTop = container.scrollHeight;
}

function enterRoom(socket) {
    var enterMsg = {
        type : "ENTER",
        roomId : roomId,
        email : email
    };

    msgSend(enterMsg);
}

 function send(e) {
    if (e.keyCode == 13 && !e.shiftKey) {
        event.preventDefault();

        sendMsg();
    }
}

function sendMsg() {
    var content = document.getElementById('chat').value;

    let risk = msgCheck(content);

    if (content != '') {
        document.getElementById('chat').value = null;

        if (risk == -1)
            content = "<부적절한 내용이 포함되어 검열되었습니다>";

        var talkMsg = {
            type : "TALK",
            roomId : roomId,
            email : email,
            message : content
        };

        msgSend(talkMsg);
    }
}

function quit() {
    var quitMsg = {
        type : "QUIT",
        roomId : roomId,
        email : email
    };

    msgSend(quitMsg)
    history.back(-1);
}

function msgSend(msg) {
    fetch(`/api/chat/message/send`, {
        method: 'POST',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        body: JSON.stringify(msg),
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json != 1)
            alert("메세지 전송 실패")
    })
    .catch((error) => {
        console.log(error)
        alert("다시 시도하여주십시오");
    })
}

function msgCheck(msg) {
    fetch(`/api/chat/message/check?message=`+msg, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json == -1)
            return -1
        else if (json == -3) {
            alert("금칙어를 사용하여 계정이 정지되었습니다");
            window.location.reload();
        }
    })
    .catch((error) => {
        alert("다시 시도하여주십시오");
    })

    return 0;
}

function fnLoad(input) {
    var file = input.files[0];
    var imgName;
    var status;

    const formData = new FormData();
    formData.append("file", file);

    fetch(`/api/chat/sendImage`, {
        method: 'POST',
        body: formData,
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        console.log(json)
        status = json['status']

        if (status != 'OK')
            alert("이미지 전송 실패");
        else {
            imgName = json['name'];

            var talkMsg = {
                type : "IMAGE",
                roomId : roomId,
                email : email,
                message : imgName
            };

            msgSend(talkMsg)
        }
    })
    .catch((error) => {
        alert("다시 시도하여주십시오");
    })
}