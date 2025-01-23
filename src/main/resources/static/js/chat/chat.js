let socket = new WebSocket("wss://www.kwanwoo.site/ws/chat");
//let socket = new WebSocket("ws://localhost:8080/ws/chat");

const roomId = document.querySelector("#room").value;
const email = document.querySelector("#email").value;
const flag = document.querySelector("#flag").value;

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
    let img = document.createElement('img');

    newMsgLi.className = "list-group-item";
    img.src = "/img/" + json.member.profile;
    img.id = "profile"
    name.innerText = json.member.name;
    newMsg.innerText = json.message;

    newMsgArea.append(img);
    newMsgArea.append(name);
    newMsgArea.append(newMsg);

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
    let content = document.querySelector('.content').value;
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