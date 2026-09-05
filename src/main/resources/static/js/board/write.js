let imgDiv = document.querySelector('.imgDiv');
let file, fidx;
let id;
let img;
let left, right, size, imageCounter;

const upload = document.getElementById("upload");
const btn = document.getElementById("btn");
const img_btn = document.getElementById("img_btn");
const previous = document.getElementById('previous');
const submit = document.getElementById('submit');
const content = document.getElementById('content')
const visibility = document.getElementById('visibility');
const visibilityGroup = document.getElementById('visibilityGroup');

const maxSize = 10;

btn.addEventListener('click', () => upload.click());

function fnSave() {
    const data = {
        content: content.value,
        visibility: visibility.value
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
            if (hasSelectedImages()) {
                img_btn.click();
            } else {
                alert("게시글이 저장되었습니다.");
                location.replace(`/board/main`);
            }
        }
    })
    .catch((error) => {
        alert("다시 시도하여주십시오.");
    });
}

function fnImgSave() {
    const uploadFormData = buildImageFormData();
    if (!uploadFormData) {
        alert("업로드할 이미지를 다시 선택하여 주십시오.");
        return;
    }

    fetch(`/api/boardImg/save?id=` + id, {
        method: 'POST',
        body: uploadFormData,
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['result'] > 0) {
            alert("게시글 사진 등록 완료");
            location.replace(`/board/main`);
        } else if (json['result'] == -2) {
            alert("게시글 사진 갯수 초과")
            fnDelete(id)
        } else if (json['result'] == -99) {
            alert(json['message']);
            fnDelete(id)
        } else {
            alert(json['message'] || "게시글 사진 등록 실패");
            fnDelete(id)
        }
    })
    .catch((error) => {
        alert("게시글 사진 등록 실패");
        if (id) fnDelete(id);
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
    btn.style.display = 'inline';
    imgDiv.style.marginTop = '300px';

    previous.style.display = 'none';
    submit.style.display = 'none';
    content.style.display = 'none';
    visibilityGroup.style.display = 'none';

    if (left)
        left.style.display = 'none';

    if (right)
        right.style.display = 'none';

    if (imageCounter) {
        imageCounter.remove();
        imageCounter = null;
    }

    file = null;
    fidx = 0;
    size = 0;
    upload.value = '';

    img.remove();
}

async function fnLoad(input) {
    const selectedFiles = Array.from(input.files || []);
    if (selectedFiles.length === 0) {
        return;
    }

    if (selectedFiles.length > maxSize) {
        alert('최대 ' + maxSize + "장의 사진만 업로드가 가능합니다")
    }

    try {
        file = await Promise.all(selectedFiles.slice(0, maxSize).map(snapshotFile));
    } catch (error) {
        console.error(error);
        file = null;
        alert("선택한 사진을 읽을 수 없습니다. 사진을 다시 선택하여 주십시오.");
        return;
    }

    size = file.length;

    imgDiv.append(document.createElement('br'));
    imgDiv.append(document.createElement('br'));

    imgDiv.style.marginTop = 0;

    btn.style.display = 'none';
    previous.style.display = 'flex';
    submit.style.display = 'flex';
    content.style.display = 'inline';
    visibilityGroup.style.display = 'block';

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

async function snapshotFile(selectedFile, index) {
    const bytes = await selectedFile.arrayBuffer();
    if (bytes.byteLength === 0) {
        throw new Error("empty image file");
    }

    return new File([bytes], resolveFileName(selectedFile, index), {
        type: selectedFile.type || "application/octet-stream",
        lastModified: selectedFile.lastModified || Date.now(),
    });
}

function resolveFileName(selectedFile, index) {
    const originalName = typeof selectedFile.name === "string" ? selectedFile.name.trim() : "";
    if (originalName) {
        return originalName;
    }

    const extensionByType = {
        "image/jpeg": "jpg",
        "image/png": "png",
        "image/gif": "gif",
    };
    const extension = extensionByType[selectedFile.type] || "bin";
    return `mobile-image-${Date.now()}-${index}.${extension}`;
}

function hasSelectedImages() {
    return Array.isArray(file)
        && file.length > 0
        && file.every(imageFile => imageFile instanceof Blob && imageFile.size > 0);
}

function buildImageFormData() {
    if (!hasSelectedImages()) {
        return null;
    }

    const uploadFormData = new FormData();
    file.forEach((imageFile, index) => {
        uploadFormData.append("file", imageFile, resolveFileName(imageFile, index));
    });
    return uploadFormData;
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
