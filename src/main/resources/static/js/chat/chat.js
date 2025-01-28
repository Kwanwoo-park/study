//let socket = new WebSocket("wss://www.kwanwoo.site/ws/chat");
let socket = new WebSocket("ws://localhost:8080/ws/chat");

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
    var msgArea = document.querySelector('.list-group-flush');

    var newMsgLi = document.createElement('li');
    var newMsgArea = document.createElement('span');
    var name = document.createElement('span');
    var newMsg = document.createElement('pre');
    var profile = document.createElement('img');
    var imgTalk = document.createElement('img');

    newMsgLi.className = "list-group-item";
    profile.src = "/img/" + json.member.profile;
    profile.id = "profile"
    name.innerText = json.member.name;
    newMsg.innerText = json.message;
    imgTalk.src = "/img/" + json.message;

    if (json.member.email == email) {
        name.style.display = 'inline-block';
        name.style.width = '95%';

        profile.align = 'right';
        imgTalk.align = 'right';

        newMsg.style.width = '95%';

        newMsgArea.style.textAlign = 'right';

        newMsgArea.append(name);
        newMsgArea.append(profile);
    }
    else {
        name.style.display = 'inline-block';
        name.style.width = '95%';

        profile.align = 'left';
        imgTalk.align = 'left';

        newMsg.style.width = '95%';

        newMsgArea.style.textAlign = 'left';

        newMsgArea.append(profile);
        newMsgArea.append(name);
    }

    if (json.type == "TALK")
        newMsgArea.append(newMsg);
    else if (json.type == "IMAGE")
        newMsgArea.append(imgTalk);

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
    if (e.keyCode == 13)
        sendMsg();
}

function sendMsg() {
    var content = document.querySelector('.content').value;

    if (content != '') {
        document.querySelector('.content').value = null;

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
    location.replace(`/chat/chatList`);
}

function fnLoad(input) {
    var file = input.files[0];
    var imgName;

    const formData = new FormData();
    formData.append("file", file);

    fetch(`/api/chat/sendImage`, {
        method: 'POST',
        body: formData,
    })
    .then((response) => response.json())
    .then((json) => {
        imgName = json['name'];

        var talkMsg = {
            type : "IMAGE",
            roomId : roomId,
            email : email,
            message : imgName
        };

        socket.send(JSON.stringify(talkMsg));
    })
    .catch((error) => {
        alert("다시 시도하여주십시오");
    })
}