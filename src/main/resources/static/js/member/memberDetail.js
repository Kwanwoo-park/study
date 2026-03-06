const follower = document.querySelector("#follower_label").innerText;
const following = document.querySelector("#following_label").innerText;
const btn = document.querySelector("#follow");
const chatting = document.querySelector("#chatting");

const url = new URL(window.location.href);
const urlParams = url.searchParams;
const email = urlParams.get('email');

const modal = document.getElementById('boardModal');
const modalBody = document.getElementById('boardModalBody');
const modalOverlay = document.getElementById('boardModalOverlay');
const modalClose = document.getElementById('boardModalClose');
const boardPrevBtn = document.getElementById('boardPrevBtn');
const boardNextBtn = document.getElementById('boardNextBtn');

let method;
let modalOpen = false;
let currentBoardId = null;
let currentBoardData = null;
let currentPrevBoardId = 0;
let currentNextBoardId = 0;
let currentImageIndex = 0;
let boardFetchController = null;
let renderVersion = 0;

if (btn && btn.innerText === 'Follow') method = "POST";
else method = "DELETE";

if (follower === '0') document.querySelector("#follower").removeAttribute('href');
if (following === '0') document.querySelector("#following").removeAttribute('href');

if (btn) {
    btn.addEventListener('click', () => {
        const data = { email: email };

        fetch(`/api/follow`, {
            method: method,
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            body: JSON.stringify(data),
            credentials: "include",
        })
        .then((response) => response.json())
        .then((json) => {
            if (json['result'] > 0) window.location.reload();
            else alert("다시 시도하여주십시오");
        })
        .catch(() => {
            alert("다시 시도하여주십시오");
        });
    });
}

if (chatting) {
    chatting.addEventListener('click', () => {
        const data = { email: email };

        fetch(`/api/chat/create`, {
            method: 'POST',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            body: JSON.stringify(data),
            credentials: "include",
        })
        .then((response) => response.json())
        .then((json) => {
            location.href = '/chat/chatRoom?roomId=' + json['room'].roomId;
        })
        .catch(() => {
            alert("다시 시도하여주십시오");
        });
    });
}

if (modalOverlay) {
    modalOverlay.addEventListener('click', () => closeBoardModal());
}

if (modalClose) {
    modalClose.addEventListener('click', () => closeBoardModal());
}

if (boardPrevBtn) {
    boardPrevBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        if (currentPrevBoardId && currentPrevBoardId !== 0) {
            openBoardModal(currentPrevBoardId);
        }
    });
}

if (boardNextBtn) {
    boardNextBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        if (currentNextBoardId && currentNextBoardId !== 0) {
            openBoardModal(currentNextBoardId);
        }
    });
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

    const date = new Date(board.registerTime);
    const dateText =
        date.getFullYear() + "년 " +
        (date.getMonth() + 1) + "월 " +
        date.getDate() + "일";

    modalBody.innerHTML = `
        <div class="modal-profile" onclick="fnProfile('${board.member.email}')">
            <img class="modal-profile-img" src="${board.member.profile}">
            <span>${escapeHtml(board.member.name)}</span>
        </div>

        <div class="modal-image-box">
            <div class="modal-image-arrow left ${currentImageIndex === 0 ? 'hidden' : ''}" onclick="modalImageLeft(event)">←</div>
            <img class="modal-main-image" id="modalMainImage" src="${currentImage.imgSrc}" alt="board image">
            <div class="modal-image-arrow right ${currentImageIndex === imageList.length - 1 ? 'hidden' : ''}" onclick="modalImageRight(event)">→</div>
        </div>

        <div class="d-flex gap-3 mb-2">
            <span onclick="fnHref(${board.id})" style="cursor:pointer;">좋아요 ${board.favorites.length}개</span>
            <span onclick="fnComment(${board.id})" style="cursor:pointer;">댓글 ${board.comment.length}개 모두 보기</span>
        </div>

        <div class="mb-2">
            <strong>${escapeHtml(board.member.name)}</strong>
        </div>

        <pre style="white-space: pre-wrap;">${escapeHtml(board.content)}</pre>

        <div class="text-muted mt-3">${dateText}</div>
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
    const leftArrow = document.querySelector('.modal-image-arrow.left');
    const rightArrow = document.querySelector('.modal-image-arrow.right');

    if (modalMainImage) {
        modalMainImage.src = imageList[currentImageIndex].imgSrc;
    }

    if (leftArrow) {
        if (currentImageIndex === 0) leftArrow.classList.add('hidden');
        else leftArrow.classList.remove('hidden');
    }

    if (rightArrow) {
        if (currentImageIndex === imageList.length - 1) rightArrow.classList.add('hidden');
        else rightArrow.classList.remove('hidden');
    }
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
    location.href = "/board/view?id=" + listId;
}

function fnComment(listId) {
    location.href = "/comment?id=" + listId;
}

function fnProfile(email) {
    location.href = "/member/search/detail?email=" + email;
}