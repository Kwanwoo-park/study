(function () {
    function update(button, saved) {
        button.dataset.saved = String(saved);
        button.setAttribute('aria-pressed', String(saved));
        button.textContent = saved ? '저장 취소' : '나만 저장';
    }

    function button(boardId, saved) {
        const element = document.createElement('button');
        element.type = 'button';
        element.className = 'btn btn-outline-secondary btn-sm';
        element.dataset.boardBookmark = String(boardId);
        update(element, saved);
        return element;
    }

    document.addEventListener('click', async event => {
        const button = event.target.closest('button[data-board-bookmark]');
        if (!button || button.disabled) return;
        button.disabled = true;
        try {
            const id = encodeURIComponent(button.dataset.boardBookmark);
            const response = await fetch(`/api/board/bookmarks/${id}`, {
                method: button.dataset.saved === 'true' ? 'DELETE' : 'PUT',
                credentials: 'include'
            });
            const data = await response.json();
            if (!response.ok) throw new Error(data.message || '게시글 저장을 변경하지 못했습니다.');
            update(button, data.saved);
            if (button.hasAttribute('data-bookmark-list') && !data.saved) window.location.reload();
        } catch (error) {
            alert(error.message);
        } finally {
            button.disabled = false;
        }
    });

    window.BoardBookmarks = {button};
})();
