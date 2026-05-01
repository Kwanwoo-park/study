(function (global) {
    function initMemberBoardModal() {
        const modal = document.getElementById('boardModal');
        const modalBody = document.getElementById('boardModalBody');
        const modalOverlay = document.getElementById('boardModalOverlay');
        const modalClose = document.getElementById('boardModalClose');
        const boardPrevBtn = document.getElementById('boardPrevBtn');
        const boardNextBtn = document.getElementById('boardNextBtn');

        if (!modal || !modalBody || !boardPrevBtn || !boardNextBtn) {
            return;
        }

        let modalOpen = false;
        let currentBoardData = null;
        let currentPrevBoardId = 0;
        let currentNextBoardId = 0;
        let currentImageIndex = 0;
        let boardFetchController = null;
        let renderVersion = 0;

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
            const canEdit = board.member.email === data.email;
            const currentImage = imageList[currentImageIndex];
            const liked = Boolean(data.like);

            modalBody.innerHTML = `
                ${canEdit ? `
                <div class="editDiv">
                    <span onclick="fnEditModal()">Edit</span>
                    <span onclick="fnEditCompleteModal(${board.id})" id="modalComplete" style="display: none;">Complete</span>
                    <br>
                    <span onclick="fnDeleteModal(${board.id})" id="modalDelete">Delete</span>
                </div>
                ` : ''}
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
                        <label class="form-label" id="like_cnt${board.id}">${like_count[board.id]}</label>
                        <label class="form-label">개</label>
                    </div>

                    <span class="name">${escapeHtml(board.member.name)}</span>
                    <pre class="modal-content-pre" id="modalContent">${escapeHtml(board.content)}</pre>
                    <textarea class="form-control" rows="5" id="modalEditContent" style="display: none;">${escapeHtml(board.content)}</textarea>

                    <div class="comment" onclick="fnComment(${board.id})">
                        <label class="form-label">댓글</label>
                        <label class="form-label" id="comment_cnt${board.id}">${comment_count[board.id]}</label>
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

        function updateBoardNavigation() {
            if (!currentPrevBoardId || currentPrevBoardId === 0) boardPrevBtn.classList.add('hidden');
            else boardPrevBtn.classList.remove('hidden');

            if (!currentNextBoardId || currentNextBoardId === 0) boardNextBtn.classList.add('hidden');
            else boardNextBtn.classList.remove('hidden');
        }

        boardPrevBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            if (currentPrevBoardId && currentPrevBoardId !== 0) {
                openBoardModal(currentPrevBoardId, false);
            }
        });

        boardNextBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            if (currentNextBoardId && currentNextBoardId !== 0) {
                openBoardModal(currentNextBoardId, false);
            }
        });

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

        function fnHref(listId) {
            if (typeof global.openFavoriteModal === 'function') {
                global.openFavoriteModal(listId);
                return;
            }

            location.href = "/favorites?id=" + listId;
        }

        function fnComment(listId) {
            if (typeof global.openCommentModal === 'function') {
                global.openCommentModal(listId);
                return;
            }

            location.href = "/comment?id=" + listId;
        }

        function fnProfile(email) {
            location.href = "/member/search/detail?email=" + email;
        }

        function fnEditModal() {
            const edit = modalBody.querySelector('.editDiv span');
            const complete = document.getElementById('modalComplete');
            const del = document.getElementById('modalDelete');
            const content = document.getElementById('modalContent');
            const editContent = document.getElementById('modalEditContent');

            if (!edit || !complete || !del || !content || !editContent) return;

            edit.style.display = 'none';
            del.style.display = 'none';
            content.style.display = 'none';
            complete.style.display = 'inline';
            editContent.style.display = 'inline';
        }

        function fnEditCompleteModal(boardId) {
            const editContent = document.getElementById('modalEditContent');
            if (!editContent) return;

            fetch(`/api/board/view`, {
                method: 'PATCH',
                headers: {
                    "Content-Type": "application/json; charset=utf-8",
                },
                body: JSON.stringify({
                    id: boardId,
                    content: editContent.value
                }),
                credentials: "include",
            })
            .then((response) => response.json())
            .then((json) => {
                if (json.result == -1) alert("부적절한 내용 감지되었습니다");
                else if (json.result == -3) {
                    alert("금칙어를 사용하여 계정이 정지되었습니다");
                    window.location.reload();
                } else if (json.result == -10) {
                    alert("다시 시도하여주십시오");
                } else {
                    openBoardModal(boardId, false);
                }
            })
            .catch(() => {
                alert("다시 시도하여주십시오");
            });
        }

        function fnDeleteModal(boardId) {
            fetch(`/api/board/view/delete?id=` + boardId, {
                method: 'DELETE',
                headers: {
                    "Content-Type": "application/json; charset=utf-8",
                },
                credentials: "include",
            })
            .then((response) => response.json())
            .then((json) => {
                if (json.result != -1) {
                    alert("삭제가 완료되었습니다");
                    location.replace(`/member/detail?email=` + json.email);
                } else {
                    alert("다시 시도하여주십시오");
                }
            })
            .catch(() => {
                alert("다시 시도하여주십시오");
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

        global.openBoardModal = openBoardModal;
        global.closeBoardModal = closeBoardModal;
        global.modalImageLeft = modalImageLeft;
        global.modalImageRight = modalImageRight;
        global.fnLike = fnLike;
        global.fnOnlyLike = fnOnlyLike;
        global.fnHref = fnHref;
        global.fnComment = fnComment;
        global.fnProfile = fnProfile;
        global.fnEditModal = fnEditModal;
        global.fnEditCompleteModal = fnEditCompleteModal;
        global.fnDeleteModal = fnDeleteModal;
    }

    global.initMemberBoardModal = initMemberBoardModal;
})(window);
