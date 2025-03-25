let imgDiv = document.querySelector('.imgDiv');
let file, fidx = 0;
let id;
let img;
let left, right, size;

const formData = new FormData();

const upload = document.getElementById("upload");
const btn = document.getElementById("btn");
const img_btn = document.getElementById("img_btn");
const previous = document.getElementById('previous');
const submit = document.getElementById('submit');
const content = document.getElementById('content')

if (btn)
    btn.addEventListener('click', () => upload.click());

function fnSave() {
    const data = {
        content: content.value
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
        location.replace(`/board/main`);
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

function fnLeft() {
    img.src = URL.createObjectURL(file[--fidx]);

    if (right.style.display === 'none')
        right.style.display = 'flex';

    if (fidx == 0)
        left.style.display = 'none';
}

function fnRight() {
    img.src = URL.createObjectURL(file[++fidx]);

    if (left.style.display === 'none')
        left.style.display = 'flex';

    if (fidx == size - 1)
        right.style.display = 'none';
}

function fnPrevious() {
    btn.style.display = 'inline';
    imgDiv.style.marginTop = '300px';

    previous.style.display = 'none';
    submit.style.display = 'none';

    img.remove();
    formData = new FormData();
}

function fnLoad(input) {
    imgArr = new Array(input.files.length);

    imgDiv.append(document.createElement('br'));
    imgDiv.append(document.createElement('br'));

    imgDiv.style.marginTop = 0;

    btn.style.display = 'none';
    previous.style.display = 'inline';
    submit.style.display = 'inline';
    content.style.display = 'inline';

    file = input.files;
    size = input.files.length;

    img = document.createElement('img');
    img.src = URL.createObjectURL(file[fidx]);

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

        right = document.createElement('button');
        right.type = "button";
        right.className = "arrow";
        right.id = 'left';
        right.onclick = function () {
            fnRight();
        };
        right.textContent = '→';

        imgDiv.append(left);
        imgDiv.append(img);
        imgDiv.append(right);
    }
    else imgDiv.append(img);


    for (let i = 0; i < size; i++) {
            formData.append("file", file[i]);
    }
}