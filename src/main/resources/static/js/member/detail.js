const follower = document.querySelector("#follower_label").innerText;
const following = document.querySelector("#following_label").innerText;

const upload = document.querySelector("#upload");
const profile = document.querySelector("#profile");

const modal = document.getElementById('boardModal');
const modalBody = document.getElementById('boardModalBody');
const modalOverlay = document.getElementById('boardModalOverlay');
const modalClose = document.getElementById('boardModalClose');
const boardPrevBtn = document.getElementById('boardPrevBtn');
const boardNextBtn = document.getElementById('boardNextBtn');

profile.addEventListener('click', () => upload.click());

let modalOpen = false;
let currentBoardId = null;
let currentBoardData = null;
let currentPrevBoardId = 0;
let currentNextBoardId = 0;
let currentImageIndex = 0;
let boardFetchController = null;
let renderVersion = 0;

let file;

if (follower == "0")
    document.querySelector("#follower").removeAttribute('href');
if (following == "0")
    document.querySelector("#following").removeAttribute('href');

function fnLoad(input) {
    file = input.files[0];

    var img = profile
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
        status = json['status'];

        if (status == 500)
            alert("사진 변경에 실패했습니다");
        else {
            alert("사진이 변경되었습니다.");
            window.location.reload();
        }
    })
    .catch((error) => {
        alert("사진 변경에 실패했습니다");
    })
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
        } else
            alert("다시 시도하여주십시오");
    })
}

if (modalOverlay) {
    modalOverlay.addEventListener('click', () => closeBoardModal());
}

if (modalClose) {
    modalClose.addEventListener('click', () => closeBoardModal());
}

window.addEventListener('popstate', async (event) => {
    const state = event.state;

    if (state && state.boardModal && state.boardId) {
        await openBoardModal(state.boardId, false);
        return;
    }

    if (modalOpen) {
        closeBoardModal(true);
    }
});

async function openBoardModal(boardId, push = true) {
    try {
        renderVersion += 1;
        const version = renderVersion;

        if (boardFetchController) {
            boardFetchController.abort();
        }

        boardFetchController = new AbortController();

        modalBody.innerHTML = '';
        modal.classList.remove('hidden');
        document.body.classList.add('modal-open');
        modalOpen = true;

        const response = await fetch(`/api/board/detail?id=${boardId}`, {
            method: 'GET',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            credentials: "include",
            signal: boardFetchController.signal,
        });

        const json = await response.json();

        if (version !== renderVersion) return;

        if (json.result !== 10) {
            alert("다시 시도하여주십시오");
            closeBoardModal(true);
            return;
        }

        currentBoardId = boardId;
        currentBoardData = json.board;
        currentPrevBoardId = json.previous;
        currentNextBoardId = json.next;
        currentImageIndex = 0;

        drawBoardModal(json);
        updateBoardNavigation();

        if (push) {
            history.pushState(
                { boardModal: true, boardId: boardId },
                '',
                `/board/view?id=${boardId}`
            );
        } else {
            history.replaceState(
                { boardModal: true, boardId: boardId },
                '',
                `/board/view?id=${boardId}`
            );
        }
    } catch (error) {
        if (error.name === 'AbortError') return;
        console.error(error);
        alert("다시 시도하여주십시오");
        closeBoardModal(true);
    }
}

function closeBoardModal(fromPopState = false) {
    if (boardFetchController) {
        boardFetchController.abort();
        boardFetchController = null;
    }

    modal.classList.add('hidden');
    modalBody.innerHTML = '';
    document.body.classList.remove('modal-open');

    modalOpen = false;
    currentBoardId = null;
    currentBoardData = null;
    currentPrevBoardId = 0;
    currentNextBoardId = 0;
    currentImageIndex = 0;

    if (!fromPopState && history.state && history.state.boardModal) {
        history.back();
    }
}

function drawBoardModal(data) {
    const board = data.board;
    const imageList = (board.img && board.img.length > 0)
        ? board.img
        : [{ imgSrc: "/img/IMG_0111.jpeg" }];

    const currentImage = imageList[currentImageIndex];
    const liked = Boolean(data.like);

    modalBody.innerHTML = `
        <div class="modal-profile" onclick="fnProfile('${escapeHtml(board.member.email)}')">
            <img class="modal-profile-img" src="${board.member.profile}">
            <span>${escapeHtml(board.member.name)}</span>
        </div>

        <div class="modal-image-box">
            <button type="button" id="modalImageLeftBtn" class="modal-image-arrow left" style="visibility: ${currentImageIndex === 0 ? 'hidden' : 'visible'};" onclick="modalImageLeft(event)">←</button>
            <img class="modal-main-image" id="modalMainImage" src="${currentImage.imgSrc}" alt="board image" ondblclick="fnOnlyLike(${board.id})">
            <button type="button" id="modalImageRightBtn" class="modal-image-arrow right" style="visibility: ${currentImageIndex === imageList.length - 1 ? 'hidden' : 'visible'};" onclick="modalImageRight(event)">→</button>
        </div>

        <div class="modal-info">
            <div class="icon-box">
                <img id="like${board.id}" src="/img/${liked ? 'ic_favorite.png' : 'ic_favorite_border.png'}" class="icon" onclick="fnLike(${board.id})">
                <img id="comment${board.id}" src="/img/ic_chat_black.png" class="icon" onclick="fnComment(${board.id})">
            </div>

            <div class="like" onclick="fnHref(${board.id})">
                <label class="form-label">좋아요</label>
                <label class="form-label" id="like_cnt${board.id}">${board.favorites.length}</label>
                <label class="form-label">개</label>
            </div>

            <span class="name">${escapeHtml(board.member.name)}</span>
            <pre class="modal-content-pre">${escapeHtml(board.content)}</pre>

            <div class="comment" onclick="fnComment(${board.id})">
                <label class="form-label">댓글</label>
                <label class="form-label" id="comment_cnt${board.id}">${board.comment.length}</label>
                <label class="form-label">개 모두 보기</label>
            </div>
        </div>
    `;
}

function modalImageLeft(event) {
    event.stopPropagation();

    if (!currentBoardData || !currentBoardData.img || currentBoardData.img.length === 0) return;
    if (currentImageIndex === 0) return;

    currentImageIndex -= 1;
    redrawModalImage();
}

function modalImageRight(event) {
    event.stopPropagation();

    if (!currentBoardData || !currentBoardData.img || currentBoardData.img.length === 0) return;
    if (currentImageIndex >= currentBoardData.img.length - 1) return;

    currentImageIndex += 1;
    redrawModalImage();
}

function redrawModalImage() {
    const imageList = (currentBoardData.img && currentBoardData.img.length > 0)
        ? currentBoardData.img
        : [{ imgSrc: "/img/IMG_0111.jpeg" }];

    const modalMainImage = document.getElementById('modalMainImage');
    const leftArrow = document.getElementById('modalImageLeftBtn');
    const rightArrow = document.getElementById('modalImageRightBtn');

    if (modalMainImage) {
        modalMainImage.src = imageList[currentImageIndex].imgSrc;
    }

    if (leftArrow) {
        leftArrow.style.visibility = currentImageIndex === 0 ? 'hidden' : 'visible';
    }

    if (rightArrow) {
        rightArrow.style.visibility = currentImageIndex === imageList.length - 1 ? 'hidden' : 'visible';
    }
}

function fnLike(listId) {
    const like = document.getElementById('like' + listId);
    const likeCnt = document.getElementById('like_cnt' + listId);
    if (!like || !likeCnt) return;

    const liked = like.src.endsWith('ic_favorite.png');
    const url = liked ? `/api/favorite/delete?id=${listId}` : `/api/favorite/like?id=${listId}`;
    const method = liked ? 'DELETE' : 'POST';

    fetch(url, {
        method: method,
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['result'] > 0) {
            likeCnt.innerText = String(parseInt(likeCnt.innerText, 10) + (liked ? -1 : 1));
            like.src = liked ? "/img/ic_favorite_border.png" : "/img/ic_favorite.png";
        } else {
            alert("다시 시도하여주십시오");
        }
    })
    .catch(() => {
        alert("다시 시도하여주십시오.");
    });
}

function fnOnlyLike(listId) {
    const like = document.getElementById('like' + listId);
    if (!like || like.src.endsWith('ic_favorite.png')) return;
    fnLike(listId);
}

function updateBoardNavigation() {
    if (!boardPrevBtn || !boardNextBtn) return;

    if (!currentPrevBoardId || currentPrevBoardId === 0) boardPrevBtn.classList.add('hidden');
    else boardPrevBtn.classList.remove('hidden');

    if (!currentNextBoardId || currentNextBoardId === 0) boardNextBtn.classList.add('hidden');
    else boardNextBtn.classList.remove('hidden');
}

if (boardPrevBtn) {
    boardPrevBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        if (currentPrevBoardId && currentPrevBoardId !== 0) {
            openBoardModal(currentPrevBoardId, false);
        }
    });
}

if (boardNextBtn) {
    boardNextBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        if (currentNextBoardId && currentNextBoardId !== 0) {
            openBoardModal(currentNextBoardId, false);
        }
    });
}

function escapeHtml(str) {
    return String(str)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

function fnHref(listId) {
    location.href = "/favorites?id=" + listId;
}

function fnComment(listId) {
    location.href = "/comment?id=" + listId;
}

function fnProfile(email) {
    location.href = "/member/search/detail?email=" + email;
}
