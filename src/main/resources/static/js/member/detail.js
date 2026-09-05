const follower = document.querySelector("#follower_label").innerText;
const following = document.querySelector("#following_label").innerText;

const upload = document.querySelector("#upload");
const profile = document.querySelector("#profile");
const container = document.querySelector('.container');
const imgGrid = document.querySelector('.imgGrid');
const memberEmail = container?.dataset.memberEmail;

const BOARD_LIMIT = 30;

let file;
let profilePreviewUrl = null;
let isPreparingProfileImage = false;
let isSavingProfileImage = false;
const profileSaveButton = document.getElementById('save');
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

initProfileActionMenu();

async function fnLoad(input) {
    if (isPreparingProfileImage || isSavingProfileImage) return;
    const selectedFile = input.files && input.files[0];
    if (!selectedFile) return;

    isPreparingProfileImage = true;
    input.disabled = profileSaveButton.disabled = true;
    try {
        const [snapshot] = await ImageUpload.snapshotFiles([selectedFile]);
        const nextPreviewUrl = URL.createObjectURL(snapshot);
        if (profilePreviewUrl) URL.revokeObjectURL(profilePreviewUrl);
        file = snapshot;
        profilePreviewUrl = nextPreviewUrl;
        profile.src = profilePreviewUrl;
        input.value = '';
        profileSaveButton.classList.remove('is-hidden');
    } catch (error) {
        input.value = '';
        alert(error.message);
    } finally {
        isPreparingProfileImage = false;
        input.disabled = profileSaveButton.disabled = false;
    }
}

function initProfileActionMenu() {
    const actions = document.querySelector('.profileActions');
    const toggle = document.querySelector('.profileMenuToggle');

    if (!actions || !toggle) {
        return;
    }

    toggle.addEventListener('click', (event) => {
        event.stopPropagation();
        const isOpen = actions.classList.toggle('open');
        toggle.setAttribute('aria-expanded', String(isOpen));
    });

    document.addEventListener('click', (event) => {
        if (!actions.contains(event.target)) {
            actions.classList.remove('open');
            toggle.setAttribute('aria-expanded', 'false');
        }
    });
}

async function fnSave() {
    if (isPreparingProfileImage || isSavingProfileImage) return;
    if (!file) {
        alert('업로드할 이미지를 다시 선택하여 주십시오.');
        return;
    }

    isSavingProfileImage = true;
    upload.disabled = profileSaveButton.disabled = true;
    try {
        const response = await fetch(`/api/member/detail/action`, {
            method: 'PATCH',
            body: ImageUpload.buildFormData([file]),
            credentials: "include",
        });
        const json = await response.json();
        const result = json['result'];

        if (!response.ok || !(result > 0)) {
            alert(json.message || "사진 변경에 실패했습니다");
        } else {
            alert("사진이 변경되었습니다.");
            window.location.reload();
        }
    } catch (error) {
        alert("사진 변경에 실패했습니다");
    } finally {
        isSavingProfileImage = false;
        upload.disabled = profileSaveButton.disabled = false;
    }
}

async function loadMoreBoards() {
    if (!imgGrid || !memberEmail || !nextCursor) {
        return;
    }

    try {
        const response = await fetch(`/api/board/member/detail?email=${encodeURIComponent(memberEmail)}&cursor=${nextCursor - 1}&limit=${BOARD_LIMIT}`, {
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
