(() => {
    const container = document.querySelector('.container');
    const grid = document.querySelector('.imgGrid');
    const email = container.dataset.memberEmail;
    let nextCursor = 1;
    let loading = false;

    initMemberBoardModal();
    initCommentModal();
    initFavoriteModal();
    initFollowerModal();
    initFollowingModal();

    const actions = document.querySelector('.profileActions');
    const toggle = document.querySelector('.profileMenuToggle');
    toggle.addEventListener('click', event => {
        event.stopPropagation();
        toggle.setAttribute('aria-expanded', String(actions.classList.toggle('open')));
    });
    document.addEventListener('click', event => {
        if (!actions.contains(event.target)) {
            actions.classList.remove('open');
            toggle.setAttribute('aria-expanded', 'false');
        }
    });

    window.addEventListener('load', loadBoards);
    container.addEventListener('scroll', () => {
        if (container.scrollTop + container.clientHeight >= container.scrollHeight - 1) loadBoards();
    });

    async function loadBoards() {
        if (loading || !nextCursor) return;
        loading = true;
        try {
            const response = await fetch(`/api/board/member/detail?email=${encodeURIComponent(email)}&cursor=${nextCursor - 1}&limit=30`, {
                credentials: 'include',
            });
            const data = await response.json();
            if (data.result <= 0) throw new Error('다시 시도하여주십시오');
            for (const board of data.boards) {
                const wrapper = document.createElement('div');
                wrapper.className = 'imgDiv';
                const image = document.createElement('img');
                image.className = 'main-image';
                image.id = `main_img${board.id}`;
                image.src = board.img.length ? board.img[0].imgSrc : '/img/IMG_0111.jpeg';
                image.loading = 'lazy';
                image.addEventListener('click', () => openBoardModal(board.id));
                wrapper.append(image);
                grid.append(wrapper);
            }
            nextCursor = data.nextCursor;
        } catch (error) {
            console.error(error);
            alert('다시 시도하여주십시오');
        } finally {
            loading = false;
        }
    }
})();
