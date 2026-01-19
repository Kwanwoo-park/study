let imgDiv = document.querySelector('.imgDiv');
let file, fidx;
let id;
let img;
let left, right, size;

let formData = new FormData();
let imgArr;

const upload = document.getElementById("upload");
const btn = document.getElementById("btn");
const img_btn = document.getElementById("img_btn");
const previous = document.getElementById('previous');
const submit = document.getElementById('submit');
const content = document.getElementById('content')

const maxSize = 10;

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
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        id = json['result'];

        if (id == -1) {
            alert("부적절한 내용 감지되었습니다");
        }
        else if (id == -3) {
            alert("금칙어를 사용하여 계정이 정지되었습니다");
            window.location.reload();
        }
        else if (id == -10) {
            alert("다시 시도하여주십시오.");
        }
        else {
            if (file)
                img_btn.click();

            alert("게시글이 저장되었습니다.");
        }
    })
    .catch((error) => {
        alert("다시 시도하여주십시오.");
    });
}

function fnImgSave() {
    fetch(`/api/boardImg/save?id=` + id, {
        method: 'POST',
        body: formData,
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['result'] > 0) {
            alert("게시글 사진 등록 완료");
            location.replace(`/board/main`);
        } else if (json['result'] == -2)
            alert("게시글 사진 갯수 초과")
        else if (json['result'] == -99)
            alert(json['message']);
        else
            alert("게시글 사진 등록 실패");

        fnDelete(id)
    })
    .catch((error) => {
        alert("게시글 사진 등록 실패");
    })
}

function fnLeft() {
    if (fidx - 1 < 0)
        return;

    img.src = URL.createObjectURL(file[--fidx]);

    if (right.style.visibility === 'hidden')
        right.style.visibility = 'visible';

    if (fidx == 0)
        left.style.visibility = 'hidden';
}

function fnRight() {
    if (fidx + 1 >= size)
            return;

    img.src = URL.createObjectURL(file[++fidx]);

    if (left.style.visibility === 'hidden')
        left.style.visibility = 'visible';

    if (fidx == size - 1)
        right.style.visibility = 'hidden';
}

function fnPrevious() {
    btn.style.display = 'inline';
    imgDiv.style.marginTop = '300px';

    previous.style.display = 'none';
    submit.style.display = 'none';
    content.style.display = 'none';

    left.style.display = 'none';
    right.style.display = 'none';

    file = null;
    fidx = 0;
    size = 0;

    img.remove();
    imgArr = null;
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

    file = Array.from(input.files);
    fidx = 0;
    size = input.files.length;

    img = document.createElement('img');
    img.src = URL.createObjectURL(file[fidx]);

    if (size > maxSize) {
        alert('최대 ' + maxSize + "장의 사진만 업로드가 가능합니다")
        size = maxSize;
    }

    if (size > 1) {
        left = document.createElement('button');
        left.type = "button";
        left.className = "arrow";
        left.id = 'left';
        left.style.visibility = 'hidden';
        left.onclick = function () {
            fnLeft();
        };
        left.textContent = '←';

        right = document.createElement('button');
        right.type = "button";
        right.className = "arrow";
        right.id = 'right';
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

    input.value = null;
}

function fnDelete(boardId) {
    fetch(`/api/board/view/delete?id=` + boardId, {
        method: 'DELETE',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        console.log(json);

        if (json['result'] != -1) {
            window.location.reload();
        }
        else
            alert("다시 시도하여주십시오");
    })
    .catch((error) => {
        alert("다시 시도하여주십시오");
    })
}