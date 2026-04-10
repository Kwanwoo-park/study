const upload = document.getElementById('upload');
const btn = document.getElementById('btn');
const imgBtn = document.getElementById('imgBtn');
const submit = document.getElementById('submit');
const url = document.getElementById('url');
const description = document.getElementById('description');
const imgDiv = document.getElementById('imgDiv');
const container = document.querySelector('.container');
const imgGrid = document.querySelector('.imgGrid');

const COLLECTION_LIMIT = 30;

let imgSrc = null;
let file;
let nextCursor = 1;
let isLoading = false;

window.onload = async function() {
    const isAllowed = await checkIp();

    if (!isAllowed) {
        return;
    }

    await loadMoreCollections();
};

if (btn) {
    btn.addEventListener('click', () => upload.click());
}

if (container) {
    container.addEventListener('scroll', () => {
        if (container.scrollTop + container.clientHeight >= container.scrollHeight - 1) {
            loadMoreCollections();
        }
    });
}

async function loadMoreCollections() {
    if (!imgGrid || !nextCursor || isLoading) {
        return;
    }

    isLoading = true;

    try {
        const response = await fetch(`/api/collection/load?cursor=${nextCursor - 1}&limit=${COLLECTION_LIMIT}`, {
            method: 'GET',
            credentials: 'include',
        });

        const json = await response.json();

        if (json.result > 0) {
            drawCollections(json.collection);
            nextCursor = json.nextCursor;

            if (!nextCursor) {
                const load = document.createElement('span')
                load.id = 'loading'
                load.innerText = '모든 게시물을 불러왔습니다';
                imgGrid.append(load);
            }
        } else {
            alert('다시 시도하여주십시오');
        }
    } catch (error) {
        console.error(error);
        alert('다시 시도하여주십시오');
    } finally {
        isLoading = false;
    }
}

function drawCollections(collections) {
    collections.forEach((item) => {
        const wrapper = document.createElement('div');
        wrapper.className = 'mainImgDiv';

        const image = document.createElement('img');
        image.className = 'main-image';
        image.src = item.imgSrc;
        image.alt = item.description ?? '';
        image.loading = 'lazy';
        image.onclick = function() {
            if (item.url) {
                window.open(item.url, '_blank');
            }
        };

        const desc = document.createElement('span');
        desc.className = 'main-desc';
        desc.innerText = item.description ?? '';

        wrapper.append(image);
        wrapper.append(desc);

        imgGrid.append(wrapper);
    });
}

async function fnSave() {
    await fnImgSave();

    if (!url || !description || !imgSrc) return;

    const data = {
        url: url.value,
        description: description.value,
        imgSrc: imgSrc
    };

    try {
        const response = await fetch(`/api/collection/save/collection`, {
            method: 'POST',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            body: JSON.stringify(data),
            credentials: "include",
        });

        const json = await response.json();

        if (json.result > 0)
            window.location.reload();
        else
            alert("다시 시도하여주십시오");
    } catch (error) {
        console.error(error);
        alert("다시 시도하여주십시오");
    }
}

async function fnImgSave() {
    const formData = new FormData();
    formData.append("file", file);

    try {
        const response = await fetch(`/api/collection/save/img`, {
            method: 'POST',
            body: formData,
            credentials: "include",
        });

        const json = await response.json();

        if (json.result < 0) {
            alert('사진 등록에 실패하였습니다');
        } else {
            imgSrc = json.imgSrc;
        }
    } catch (error) {
        console.error(error);
        alert("다시 시도하여주십시오");
    }
}

function fnLoad(input) {
    file = input.files[0];

    const preview = document.createElement('img');
    preview.className = 'save-img';
    preview.src = URL.createObjectURL(file);

    btn.style.display = 'none';
    submit.style.display = 'inline';

    const oldPreview = imgDiv.querySelector('.save-img');
    if (oldPreview) {
        oldPreview.remove();
    }

    imgDiv.append(preview);
}

async function checkIp() {
    try {
        const response = await fetch(`/api/collection/check`, {
            method: 'GET',
            credentials: "include",
        });

        const json = await response.json();

        if (json.result < 0) {
            if (await saveIP() !== 0) {
                window.location.reload();
            }

            return false;
        }

        return true;
    } catch (error) {
        console.error(error);
        alert("다시 시도하여 주십시오");
        return false;
    }
}

async function saveIP() {
    try {
        const response = await fetch(`/api/collection/save/ip`, {
            method: 'POST',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            credentials: 'include',
        });

        const json = await response.json();

        if (json.result < 0) {
            alert("다시 시도하여 주십시오");
            return 0;
        } else {
            return 1;
        }
    } catch (error) {
        console.error(error);
        alert('다시 시도하여 주십시오');
        return 0;
    }
}
