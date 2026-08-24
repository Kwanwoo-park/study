(function () {
    const deleteButton = document.getElementById('bookReviewDelete');
    if (!deleteButton) return;

    const message = document.getElementById('bookReviewMessage');
    deleteButton.addEventListener('click', async function () {
        const reviewId = deleteButton.dataset.reviewId;
        if (!reviewId || !window.confirm('이 독후감을 삭제하시겠습니까?')) return;

        deleteButton.disabled = true;
        try {
            const response = await fetch(`/api/book-reviews/${encodeURIComponent(reviewId)}`, {
                method: 'DELETE',
                credentials: 'include'
            });
            const body = await response.json();
            if (!response.ok || Number(body.result) < 0) {
                throw new Error(body.message || '독후감을 삭제하지 못했습니다.');
            }
            window.location.replace('/book-reviews');
        } catch (error) {
            message.textContent = error.message || '독후감을 삭제하지 못했습니다.';
            message.classList.add('error');
            deleteButton.disabled = false;
        }
    });
})();
