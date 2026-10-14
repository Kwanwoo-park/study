(function (global) {
    let nextId = 0;
    let resizeFrame;

    function updateOverflow(content) {
        if (!content.isConnected || !content.clientWidth) return;

        const button = content.nextElementSibling;
        const lineHeight = parseFloat(getComputedStyle(content).lineHeight);
        button.hidden = content.scrollHeight <= lineHeight * 3 + 1;
    }

    function initBoardContent(content) {
        if (!content || content.classList.contains('board-content-text')) return;

        const wrapper = document.createElement('div');
        wrapper.className = 'board-content';
        content.classList.add('board-content-text', 'is-collapsed');
        if (!content.id) content.id = `board-content-${++nextId}`;

        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'board-content-toggle';
        button.textContent = '자세히 보기';
        button.hidden = true;
        button.setAttribute('aria-controls', content.id);
        button.setAttribute('aria-expanded', 'false');
        button.addEventListener('click', () => {
            const collapsed = content.classList.toggle('is-collapsed');
            button.textContent = collapsed ? '자세히 보기' : '접기';
            button.setAttribute('aria-expanded', String(!collapsed));
        });

        content.before(wrapper);
        wrapper.append(content, button);
        requestAnimationFrame(() => updateOverflow(content));
    }

    global.addEventListener('resize', () => {
        cancelAnimationFrame(resizeFrame);
        resizeFrame = requestAnimationFrame(() => {
            document.querySelectorAll('.board-content-text').forEach(updateOverflow);
        });
    });

    global.initBoardContent = initBoardContent;
})(window);
