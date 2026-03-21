const upload = document.getElementById('upload');
const btn = document.getElementById('btn');
const imgBtn = document.getElementById('imgBtn');
const submit = document.getElementById('submit');
const url = document.getElementById('url');
const description = document.getElementById('description');

const imgDiv = document.getElementById('imgDiv');

let imgSrc = null;
let file;

btn.addEventListener('click' () => upload.click());

async function fnSave() {
    if (!url || !description || !imgSrc) return;

    const data = {
        url: url.value,
        description: description.value,
        imgSrc: imgSrc
    }

    try {
        const response = await fetch(`/api/collection/save`, {
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
        const response = await fetch(`/api/collection/img`, {
            method: 'POST',
            body: formData,
            credentials: "include",
        });

        const json = await response.json();

        if (json.result < 0) {
            alert('사진 등록에 실패하였습니다')
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

    const img = document.createElement('img');
    img.src = URL.createObjectURL(file);

    btn.style.display = 'none';
    submit.style.display = 'inline';

    imgDiv.append(img);
}