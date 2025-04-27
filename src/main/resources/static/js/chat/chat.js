let socket = new WebSocket("wss://www.kwanwoo.site/ws/chat");
//let socket = new WebSocket("ws://localhost:8080/ws/chat");

const roomId = document.querySelector("#room").value;
const email = document.querySelector("#email").value;
const flag = document.querySelector("#flag").value;

const upload = document.getElementById("upload");
const btn = document.getElementById("btn");

if (btn)
    btn.addEventListener('click', () => upload.click());

socket.onopen = function(e) {
    console.log('open server!')

    if (flag == 'true')
        enterRoom(socket);

    window.scrollTo(0, document.body.scrollHeight);
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

    profile.src = "/img/" + json.member.profile;
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
        imgTalk.src = "/img/" + json.message;
        imgTalk.className = "chatimg";

        newMsgArea.append(imgTalk);
    }
    else {
        newMsg.innerText = json.message;
        newMsgArea.append(newMsg);
    }

    newMsgLi.append(newMsgArea);

    msgArea.append(newMsgLi);

    window.scrollTo(0, document.body.scrollHeight);
}

function enterRoom(socket) {
    var enterMsg = {
        type : "ENTER",
        roomId : roomId,
        email : email
    };

    socket.send(JSON.stringify(enterMsg));
}

 function send(e) {
    if (e.keyCode == 13 && !e.shiftKey) {
        event.preventDefault();

        sendMsg();
    }
}

function sendMsg() {
    var content = document.getElementById('chat').value;

    if (content != '') {
        document.getElementById('chat').value = null;

        var talkMsg = {
            type : "TALK",
            roomId : roomId,
            email : email,
            message : content
        };

        socket.send(JSON.stringify(talkMsg));
    }
}

function quit() {
    var quitMsg = {
        type : "QUIT",
        roomId : roomId,
        email : email
    };

    socket.send(JSON.stringify(quitMsg));
    history.back(-1);
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
    })
    .then((response) => response.json())
    .then((json) => {
        status = json['status']

        if (status == 500)
            alert("이미지 전송 실패");
        else {
            imgName = json['name'];

            var talkMsg = {
                type : "IMAGE",
                roomId : roomId,
                email : email,
                message : imgName
            };

            socket.send(JSON.stringify(talkMsg));
        }
    })
    .catch((error) => {
        alert("다시 시도하여주십시오");
    })
}