document.addEventListener('DOMContentLoaded', function() {
    const diaryList = document.getElementById('diaryList');
    const sentinel = document.getElementById('diaryScrollSentinel');
    const status = document.getElementById('diaryLoadStatus');
    const emptyMessage = document.getElementById('diaryEmptyMessage');
    const searchForm = document.getElementById('diarySearchForm');
    const searchInput = document.getElementById('diarySearchInput');
    const searchClearButton = document.getElementById('diarySearchClearButton');
    const diaryCount = document.getElementById('diaryCount');

    if (!diaryList || !sentinel || !status || !searchForm || !searchInput) return;

    let nextPage = Number(diaryList.dataset.nextPage || 0);
    let hasNext = diaryList.dataset.hasNext === 'true';
    let loading = false;
    let searchTitle = '';
    let requestGeneration = 0;
    let activeController = null;

    function formatDate(value) {
        if (!value) return '';
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return value;

        const pad = number => String(number).padStart(2, '0');
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} `
                + `${pad(date.getHours())}:${pad(date.getMinutes())}`;
    }

    function appendDiary(diary) {
        const link = document.createElement('a');
        link.className = 'list-group-item list-group-item-action';
        link.href = `/diary/write?id=${encodeURIComponent(diary.id)}`;

        const row = document.createElement('div');
        row.className = 'd-flex justify-content-between align-items-center';

        const titleGroup = document.createElement('div');
        titleGroup.className = 'diary-title-group';

        if (diary.visibility === 'PRIVATE') {
            const lock = document.createElement('img');
            lock.className = 'diary-private-lock';
            lock.src = '/img/diary/private-lock.png';
            lock.alt = '비공개';
            lock.title = '비공개 일기';
            titleGroup.append(lock);
        }

        const title = document.createElement('strong');
        title.textContent = diary.title;
        titleGroup.append(title);

        const registerTime = document.createElement('small');
        registerTime.textContent = formatDate(diary.registerTime);

        row.append(titleGroup, registerTime);
        link.append(row);
        diaryList.append(link);
    }

    function showRetry(message) {
        status.textContent = message;
        const retryButton = document.createElement('button');
        retryButton.type = 'button';
        retryButton.className = 'btn btn-outline-secondary btn-sm diary-retry-button';
        retryButton.textContent = '다시 시도';
        retryButton.addEventListener('click', loadNextPage, { once: true });
        status.append(retryButton);
    }

    async function loadNextPage() {
        if (loading || !hasNext) return;

        loading = true;
        let loadSucceeded = false;
        const generation = requestGeneration;
        const requestedPage = nextPage;
        const requestedTitle = searchTitle;
        activeController = new AbortController();
        observer.unobserve(sentinel);
        status.textContent = '이전 일기를 불러오는 중입니다...';

        try {
            const endpoint = requestedTitle
                    ? `/api/diary/search?title=${encodeURIComponent(requestedTitle)}&page=${requestedPage}`
                    : `/api/diary/list?page=${requestedPage}`;
            const response = await fetch(endpoint, {
                method: 'GET',
                credentials: 'include',
                signal: activeController.signal
            });
            const body = await response.json();
            if (!response.ok) {
                throw new Error(body.message || '일기를 불러오지 못했습니다');
            }
            if (generation !== requestGeneration) return;

            (body.diaries || []).forEach(appendDiary);
            hasNext = body.hasNext === true;
            nextPage = Number(body.nextPage || 0);
            const totalCount = Number(body.totalCount || 0);
            diaryCount.textContent = String(totalCount);
            const hasDiary = diaryList.querySelector('.list-group-item') !== null;
            emptyMessage.textContent = searchTitle
                    ? '검색 결과가 없습니다.'
                    : '작성한 일기가 없습니다.';
            emptyMessage.classList.toggle('is-hidden', hasDiary);
            status.textContent = !hasDiary || hasNext ? '' : '모든 일기를 불러왔습니다.';
            loadSucceeded = true;
        } catch (error) {
            if (error.name === 'AbortError') return;
            showRetry(error.message);
        } finally {
            if (generation !== requestGeneration) return;
            loading = false;
            activeController = null;
            if (hasNext && loadSucceeded) observer.observe(sentinel);
        }
    }

    function startQuery(title) {
        requestGeneration += 1;
        if (activeController) activeController.abort();

        observer.unobserve(sentinel);
        loading = false;
        searchTitle = title.trim();
        nextPage = 0;
        hasNext = true;
        diaryList.replaceChildren();
        emptyMessage.classList.add('is-hidden');
        status.textContent = '';
        searchClearButton.classList.toggle('is-hidden', searchTitle.length === 0);
        loadNextPage();
    }

    const observer = new IntersectionObserver(function(entries) {
        if (entries.some(entry => entry.isIntersecting)) {
            loadNextPage();
        }
    }, {
        rootMargin: '0px 0px 240px 0px'
    });

    if (hasNext) {
        observer.observe(sentinel);
    }

    searchForm.addEventListener('submit', function(event) {
        event.preventDefault();
        startQuery(searchInput.value);
    });

    searchClearButton.addEventListener('click', function() {
        searchInput.value = '';
        startQuery('');
        searchInput.focus();
    });
});
