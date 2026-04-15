const follower = document.querySelector("#follower_label").innerText;
const following = document.querySelector("#following_label").innerText;

const upload = document.querySelector("#upload");
const profile = document.querySelector("#profile");
const container = document.querySelector('.container');
const imgGrid = document.querySelector('.imgGrid');
const memberEmail = container?.dataset.memberEmail;

const BOARD_LIMIT = 30;

let file;
let nextCursor = 1;

if (profile && upload) {
    profile.addEventListener('click', () => upload.click());
}

window.addEventListener('load', () => {
    loadMoreBoards();
});

if (container) {
    container.addEventListener('scroll', () => {
        if (container.scrollTop + container.clientHeight >= container.scrollHeight - 1) {
            loadMoreBoards();
        }
    });
}

if (typeof initMemberBoardModal === 'function') {
    initMemberBoardModal();
}

if (typeof initCommentModal === 'function') {
    initCommentModal();
}

if (typeof initFavoriteModal === 'function') {
    initFavoriteModal();
}

if (typeof initFollowerModal === 'function') {
    initFollowerModal();
}

if (typeof initFollowingModal === 'function') {
    initFollowingModal();
}

function fnLoad(input) {
    file = input.files[0];

    const img = profile;
    img.src = URL.createObjectURL(file);

    document.getElementById("save").style.display = 'inline';
}

function fnSave() {
    const formData = new FormData();
    formData.append("file", file);

    fetch(`/api/member/detail/action`, {
        method: 'PATCH',
        body: formData,
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        const result = json['result'];

        if (result < 0) {
            alert("사진 변경에 실패했습니다");
        } else {
            alert("사진이 변경되었습니다.");
            window.location.reload();
        }
    })
    .catch(() => {
        alert("사진 변경에 실패했습니다");
    });
}

function fnLogout() {
    fetch(`/api/member/logout`, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['result'] > 0) {
            location.replace(`/member/login`);
        } else {
            alert("다시 시도하여주십시오");
        }
    });
}

async function loadMoreBoards() {
    if (!imgGrid || !memberEmail || !nextCursor) {
        return;
    }

    try {
        const response = await fetch(`/api/member/detail/boards?email=${encodeURIComponent(memberEmail)}&cursor=${nextCursor - 1}&limit=${BOARD_LIMIT}`, {
            method: 'GET',
            credentials: 'include',
        });

        const json = await response.json();

        if (json.result > 0) {
            drawBoards(json.boards);
            nextCursor = json.nextCursor;
        } else {
            alert("다시 시도하여주십시오");
        }
    } catch (error) {
        console.error(error);
        alert("다시 시도하여주십시오");
    }
}

function drawBoards(boards) {
    boards.forEach((board) => {
        const wrapper = document.createElement('div');
        wrapper.className = 'imgDiv';

        const image = document.createElement('img');
        image.className = 'main-image';
        image.id = `main_img${board.id}`;
        image.src = board.img.length === 0 ? '/img/IMG_0111.jpeg' : board.img[0].imgSrc;
        image.loading = 'lazy';
        image.onclick = function () {
            openBoardModal(board.id);
        };

        wrapper.append(image);
        imgGrid.append(wrapper);
    });
}
