let imgDiv = document.querySelector('.mb-3:nth-child(2)');
let file;
let id;

const formData = new FormData();

const upload = document.getElementById("upload");
const btn = document.getElementById("btn");
const save = document.getElementById("save");
const img_btn = document.getElementById("img_btn");

if (btn)
    btn.addEventListener('click', () => upload.click());

function fnSave() {
    const data = {
        title: document.getElementById('title').value,
        content: document.getElementById('content').value
    }

    fetch(`/api/board/write`, {
        method: 'POST',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        body: JSON.stringify(data),
    })
    .then((response) => response.json())
    .then((json) => {
        id = json['id'];

        if (file)
            img_btn.click();

        alert("게시글이 저장되었습니다.");
        location.replace(`/board/list`);
    })
    .catch((error) => {
        alert("다시 시도하여주십시오.");
    });
}

function fnLoad(input) {
    let img;
    imgArr = new Array(input.files.length);

    imgDiv.append(document.createElement('br'));
    imgDiv.append(document.createElement('br'));

    file = input.files;

    for (let i = 0; i < input.files.length; i++) {
        img = document.createElement('img');
        img.src = URL.createObjectURL(file[i]);

        imgDiv.append(img);

        formData.append("file", file[i]);
    }
}

function fnImgSave() {
    fetch(`/api/boardImg/save?id=` + id, {
        method: 'POST',
        body: formData,
    })
    .then((response) => response.json())
    .then((json) => {
        alert("게시글 사진 등록 완료");
    })
    .catch((error) => {
        alert("게시글 사진 등록 실패");
    })
}