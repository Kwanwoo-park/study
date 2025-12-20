const url = new URL(window.location.href)
const urlParams = url.searchParams

const roomId = document.querySelector("#room").value;
const email = document.querySelector("#email").value;
const flag = document.querySelector("#flag").value;

const upload = document.getElementById("upload");
const btn = document.getElementById("btn");

let container = document.querySelector(".container");

if (btn)
    btn.addEventListener('click', () => upload.click());

window.onload = function() {
    loadMoreChat();
    container.scrollTop = container.scrollHeight;
}

let socket = new SockJS("http://localhost:8080/ws/chat")
//let socket = new SockJS("https://www.kwanwoo.site/ws/chat")

const client = Stomp.over(socket)

client.connect({}, onConnected, onError);

function onConnected() {
    if (flag == 'true')
        enterRoom(socket);

    client.subscribe("/sub/chat/room/" + roomId, onMessageReceived);
}

function onError(error) {
    console.error(error);
}

function onMessageReceived(e) {
    console.clear();

    const json = JSON.parse(e.body);

    fnDraw(json)

    container.scrollTop = container.scrollHeight;
}

async function loadMoreChat() {
    try {
        const res = await fetch(`/api/chat/load?roomId` + roomId, {
            method: 'GET',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            credentials: "include",
        });
        const data = await res.json();

        if (data['result'] > 0)
            fnDraw(data)
        else
            alert('다시 시도하여주십시오')
    } catch (e) {
        console.error('로드 오류', e);
    }
}

function enterRoom() {
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
    client.send("/api/chat/message/send", {}, JSON.stringify(msg));

    console.clear()
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
        if (json['result'] == -1)
            return -1
        else if (json['result'] == -3) {
            alert("금칙어를 사용하여 계정이 정지되었습니다");
            window.location.reload();
        } else if (json['result'] == -10) {
            alert("다시 시도하여주십시오");
        }
    })
    .catch((error) => {
        alert("다시 시도하여주십시오");
    })

    return 0;
}

function fnDraw(data) {
    let msgArea = document.querySelector('.list-group-flush');

    let newMsgLi = document.createElement('li');
    let newMsgArea = document.createElement('span');
    let name = document.createElement('span');
    let newMsg = document.createElement('pre');
    let profile = document.createElement('img');
    let imgTalk = document.createElement('img');

    newMsgLi.className = "list-group-item";

    profile.src = data.member.profile;
    profile.className = "profile";

    name.innerText = data.member.name;
    name.className = 'chatname';

    if (data.member.email == email) {
        profile.align = 'right';
        imgTalk.align = 'right';

        newMsgArea.className = 'right';

        newMsgArea.append(name);
        newMsgArea.append(profile);
    } else {
        profile.align = 'left';
        imgTalk.align = 'left';

        newMsgArea.className = 'left';

        newMsgArea.append(profile);
        newMsgArea.append(name);
    }

    if (data.type == "IMAGE") {
        const img_idx = document.createElement('input')
        img_idx.type = 'hidden'
        img_idx.id = 'idx' + data.id;
        img_idx.value = 0;

        newMsgArea.append(img_idx)

        if (img[data.id].length != 1) {
            const button = document.createElement('button')
            button.className = 'btn'
            button.type = 'button'
            button.id = 'left' + data.id;
            button.style.visibility = 'hidden'
            button.onclick = function() {
                fnLeft(data.id, img[data.id])
            }

            button.innerText = '←'
            newMsgArea.append(button);
        }

        imgTalk.src = data.message;
        imgTalk.id = 'img' + data.id;
        imgTalk.className = "chatimg";

        newMsgArea.append(imgTalk);

        if (img[data.id] > 1) {
            const button = document.createElement('button')
            button.className = 'btn'
            button.type = 'button'
            button.id = 'right' + data.id
            button.onclick = function() {
                fnRight(data.id, img[data.id])
            }

            button.innerText = '→'
            newMsgArea.append(button)
        }
    } else {
        newMsg.innerText = data.message;
        newMsgArea.append(newMsg);
    }

    newMsgLi.append(newMsgArea);

    msgArea.append(newMsgLi);
}

function fnLeft(id, arr) {
    const image = document.getElementById('img' + id);
    const idx = document.getElementById('idx' + id);
    const left_arrow = document.getElementById('left' + id);
    const right_arrow = document.getElementById('right' + id);

    if (parseInt(idx.value) -1 < 0)
        return;

    image.src = arr[parseInt(idx.value) -1].imgSrc;
    img_id.value = parseInt(idx.value) + 1;

    if (right_arrow.style.visibility == 'hidden')
        right_arrow.style.visibility = 'visible';

    if (parseInt(idx.value) == 0)
        left_arrow.style.visibility = 'hidden';
}

function fnRight(id, arr) {
    const image = document.getElementById('img' + id);
    const idx = document.getElementById('idx' + id);
    const left_arrow = document.getElementById('left' + id);
    const right_arrow = document.getElementById('right' + id);

    if (parseInt(idx.value) + 1 >= arr.length)
        return;

    image.src = arr[parseInt(idx.value) +1].imgSrc;
    img_id.value = parseInt(idx.value) + 1;

    if (left_arrow.style.visibility === 'hidden')
        left_arrow.style.visibility = 'visible'

    if (parseInt(idx.value) == arr.length-1)
        right_arrow.style.visibility = 'hidden'
}

function fnLoad(input) {
    let file = input.files[0];
    let imgName;
    let status;

    const formData = new FormData();
    formData.append("file", file);

    fetch(`/api/chat/sendImage`, {
        method: 'POST',
        body: formData,
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['result'] > 0) {
            let talkMsg = {
                id: json['messageId'],
                type : "IMAGE",
                roomId : roomId,
                email : email,
                message: json['list'][0]
            };

            msgSend(talkMsg)
        }
        else
            alert("이미지 전송 실패");
    })
    .catch((error) => {
        alert("다시 시도하여주십시오");
    })
}