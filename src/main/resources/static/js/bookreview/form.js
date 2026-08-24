(function () {
    const form = document.getElementById('bookReviewForm');
    if (!form) return;

    const reviewTitle = document.getElementById('reviewTitle');
    const bookTitle = document.getElementById('bookTitle');
    const bookAuthor = document.getElementById('bookAuthor');
    const rating = document.getElementById('rating');
    const finishedDate = document.getElementById('finishedDate');
    const content = document.getElementById('content');
    const contentCount = document.getElementById('bookReviewContentCount');
    const message = document.getElementById('bookReviewMessage');
    const submit = document.getElementById('bookReviewSubmit');

    updateContentCount();
    content.addEventListener('input', updateContentCount);
    form.addEventListener('submit', saveReview);

    function updateContentCount() {
        contentCount.textContent = content.value.length.toLocaleString('ko-KR');
    }

    async function saveReview(event) {
        event.preventDefault();
        const reviewId = form.dataset.reviewId;
        const payload = {
            reviewTitle: reviewTitle.value.trim(),
            bookTitle: bookTitle.value.trim(),
            bookAuthor: bookAuthor.value.trim(),
            rating: Number(rating.value),
            finishedDate: finishedDate.value || null,
            content: content.value.trim()
        };

        if (!payload.reviewTitle || !payload.bookTitle || !payload.bookAuthor || !payload.content) {
            showMessage('필수 항목을 모두 입력해주세요.', 'error');
            return;
        }

        submit.disabled = true;
        showMessage('독후감을 저장하는 중입니다.', '');
        try {
            const response = await fetch(reviewId
                    ? `/api/book-reviews/${encodeURIComponent(reviewId)}`
                    : '/api/book-reviews', {
                method: reviewId ? 'PATCH' : 'POST',
                headers: {'Content-Type': 'application/json; charset=utf-8'},
                credentials: 'include',
                body: JSON.stringify(payload)
            });
            const body = await response.json();
            if (!response.ok || Number(body.result) < 0) {
                throw new Error(body.message || '독후감을 저장하지 못했습니다.');
            }
            window.location.href = `/book-reviews/${encodeURIComponent(body.result)}`;
        } catch (error) {
            showMessage(error.message || '독후감을 저장하지 못했습니다.', 'error');
            submit.disabled = false;
        }
    }

    function showMessage(text, type) {
        message.textContent = text;
        message.classList.remove('error', 'success');
        if (type) message.classList.add(type);
    }
})();
