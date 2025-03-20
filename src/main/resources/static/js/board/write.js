let imgDiv = document.querySelector('.imgDiv');
let file;
let id;

let left, right, size;

const formData = new FormData();

const upload = document.getElementById("upload");
const btn = document.getElementById("btn");
const img_btn = document.getElementById("img_btn");
const previous = document.getElementById('previous');
const submit = document.getElementById('submit');

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

function fnLoad(input) {
    let img;
    imgArr = new Array(input.files.length);

    imgDiv.append(document.createElement('br'));
    imgDiv.append(document.createElement('br'));

    imgDiv.style.marginTop = 0;

    btn.style.display = 'none';
    previous.style.display = 'inline';
    submit.style.display = 'inline';

    file = input.files;
    size = input.files.length;

    img = document.createElement('img');
    img.src = URL.createObjectURL(file[0]);

    imgDiv.append(img);

    if (size > 1) {
        left = document.createElement('button');
        left.type = "button";
        left.className = "arrow";
        left.id = 'left';
        left.style.display = 'none';
        left.onclick = function () {
            fnLeft();
        };
        left.textContent = '←';
        imgDiv.append(left);

        right = document.createElement('button');
        right.type = "button";
        right.className = "arrow";
        right.id = 'left';
        right.onclick = function () {
            fnLeft();
        };
        right.textContent = '→';

        imgDiv.append(right);
    }

    for (let i = 0; i < size; i++) {
            formData.append("file", file[i]);
    }
}