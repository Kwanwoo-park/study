const follower = document.querySelector("#follower_label").innerText;
const following = document.querySelector("#following_label").innerText;

const upload = document.querySelector("#upload");
const profile = document.querySelector("#profile");

let file;

if (profile && upload) {
    profile.addEventListener('click', () => upload.click());
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
        const status = json['status'];

        if (status === 500) {
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
