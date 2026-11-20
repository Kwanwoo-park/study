const imgDiv = document.querySelector('.imgDiv');
let file = [], fidx = 0;
let id;
let img;
let left, right, size, imageCounter;
let isPreparingImages = false;
let isPublishing = false;

const upload = document.getElementById("upload");
const btn = document.getElementById("btn");
const previous = document.getElementById('previous');
const submit = document.getElementById('submit');
const content = document.getElementById('content')
const visibility = document.getElementById('visibility');
const boardDraft = window.TextDraft?.bind(document.getElementById('boardWriteForm'));

const maxSize = 10;

btn.addEventListener('click', () => upload.click());

function fnSave() {
    if (isPreparingImages || isPublishing) return;
    setPublishing(true);
    const data = {
        content: content.value,
        visibility: visibility.value
    }

    return fetch(`/api/board/write`, {
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
        else if (id > 0) {
            if (file.length) {
                return fnImgSave();
            } else {
                boardDraft?.clear();
                alert("게시글이 저장되었습니다.");
                window.location.href = `/board/main`;
            }
        } else alert(json.message || '게시글을 저장하지 못했습니다.');
    })
    .catch((error) => {
        alert("다시 시도하여주십시오.");
    }).finally(() => setPublishing(false));
}

function setPublishing(active) {
    isPublishing = active;
    submit.disabled = btn.disabled = upload.disabled = previous.disabled = visibility.disabled = active;
    content.readOnly = active;
}

function fnImgSave() {
    if (isPreparingImages) return;
    if (!file.length) {
        alert("업로드할 이미지를 다시 선택하여 주십시오.");
        return;
    }

    return fetch(`/api/boardImg/save?id=` + id, {
        method: 'POST',
        body: ImageUpload.buildFormData(file),
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['result'] > 0) {
            boardDraft?.clear();
            alert("게시글 사진 등록 완료");
            window.location.href = `/board/main`;
        } else if (json['result'] == -2) {
            alert("게시글 사진 갯수 초과")
            return fnDelete(id);
        } else if (json['result'] == -99) {
            alert(json['message']);
            return fnDelete(id);
        } else {
            alert(json['message'] || "게시글 사진 등록 실패");
            return fnDelete(id);
        }
    })
    .catch((error) => {
        alert("게시글 사진 등록 실패");
        if (id) return fnDelete(id);
    })
}

function fnLeft() {
    if (fidx - 1 < 0)
        return;

    img.src = URL.createObjectURL(file[--fidx]);
    updateImageCounter();

    if (right.style.visibility === 'hidden')
        right.style.visibility = 'visible';

    if (fidx == 0)
        left.style.visibility = 'hidden';
}

function fnRight() {
    if (fidx + 1 >= size)
            return;

    img.src = URL.createObjectURL(file[++fidx]);
    updateImageCounter();

    if (left.style.visibility === 'hidden')
        left.style.visibility = 'visible';

    if (fidx == size - 1)
        right.style.visibility = 'hidden';
}

function updateImageCounter() {
    if (imageCounter)
        imageCounter.innerText = `${fidx + 1} / ${size}`;
}

function fnPrevious() {
    if (isPreparingImages || isPublishing) return;
    btn.style.display = 'inline';
    previous.style.display = 'none';

    if (left)
        left.style.display = 'none';

    if (right)
        right.style.display = 'none';

    if (imageCounter) {
        imageCounter.remove();
        imageCounter = null;
    }

    file = [];
    fidx = 0;
    size = 0;
    upload.value = '';

    img.remove();
}

async function fnLoad(input) {
    if (isPreparingImages || isPublishing) return;
    const selectedFiles = Array.from(input.files);
    if (selectedFiles.length === 0) {
        return;
    }

    if (selectedFiles.length > maxSize) {
        alert('최대 ' + maxSize + "장의 사진만 업로드가 가능합니다")
    }

    isPreparingImages = true;
    upload.disabled = btn.disabled = submit.disabled = previous.disabled = true;
    try {
        file = await ImageUpload.snapshotFiles(selectedFiles.slice(0, maxSize));
    } catch (error) {
        console.error(error);
        input.value = '';
        alert("선택한 사진을 읽을 수 없습니다. 사진을 다시 선택하여 주십시오.");
        return;
    } finally {
        isPreparingImages = false;
        upload.disabled = btn.disabled = submit.disabled = previous.disabled = false;
    }

    size = file.length;

    btn.style.display = 'none';
    previous.style.display = 'flex';

    fidx = 0;

    img = document.createElement('img');
    img.src = URL.createObjectURL(file[fidx]);

    if (size > 1 && typeof initImageSwipe === 'function') {
        initImageSwipe(img, {
            canPrevious: () => fidx > 0,
            canNext: () => fidx < size - 1,
            onPrevious: fnLeft,
            onNext: fnRight,
        });
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

        imageCounter = document.createElement('span');
        imageCounter.className = 'image-counter';
        imageCounter.innerText = `1 / ${size}`;
        imgDiv.append(imageCounter);
    }
    else imgDiv.append(img);

}

function fnDelete(boardId) {
    return fetch(`/api/board/view/delete?id=` + boardId, {
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
