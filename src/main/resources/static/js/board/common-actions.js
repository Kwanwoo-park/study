function fnLeft(listId, imageArr, skipAnimation) {
    if (!Array.isArray(imageArr) || imageArr.length === 0) return;

    const mainImage = document.getElementById('main_img' + listId);
    const imgId = document.getElementById('img' + listId);
    const leftArrow = document.getElementById('left' + listId);
    const rightArrow = document.getElementById('right' + listId);

    if (!mainImage || !imgId) return;

    const currentIndex = parseInt(imgId.value, 10);
    if (Number.isNaN(currentIndex) || currentIndex <= 0) return;
    if (!skipAnimation && animateImageSwipe(mainImage, 'previous')) return;

    const nextIndex = currentIndex - 1;
    mainImage.src = imageArr[nextIndex].imgSrc;
    imgId.value = nextIndex;
    updateBoardImageIndicator(listId, nextIndex, imageArr.length);
    preloadAdjacentImages(imageArr, nextIndex);

    if (rightArrow) {
        rightArrow.classList.remove('is-invisible');
    }

    if (leftArrow && nextIndex === 0) {
        leftArrow.classList.add('is-invisible');
    }
}

function fnRight(listId, imageArr, skipAnimation) {
    if (!Array.isArray(imageArr) || imageArr.length === 0) return;

    const mainImage = document.getElementById('main_img' + listId);
    const imgId = document.getElementById('img' + listId);
    const leftArrow = document.getElementById('left' + listId);
    const rightArrow = document.getElementById('right' + listId);

    if (!mainImage || !imgId) return;

    const currentIndex = parseInt(imgId.value, 10);
    if (Number.isNaN(currentIndex) || currentIndex >= imageArr.length - 1) return;
    if (!skipAnimation && animateImageSwipe(mainImage, 'next')) return;

    const nextIndex = currentIndex + 1;
    mainImage.src = imageArr[nextIndex].imgSrc;
    imgId.value = nextIndex;
    updateBoardImageIndicator(listId, nextIndex, imageArr.length);
    preloadAdjacentImages(imageArr, nextIndex);

    if (leftArrow) {
        leftArrow.classList.remove('is-invisible');
    }

    if (rightArrow && nextIndex === imageArr.length - 1) {
        rightArrow.classList.add('is-invisible');
    }
}

function updateBoardImageIndicator(listId, currentIndex, totalCount) {
    const indicator = document.getElementById('imageCounter' + listId);
    if (!indicator) return;

    indicator.innerText = `${currentIndex + 1} / ${totalCount}`;
}

function enableBoardImageSwipe(listId, imageArr) {
    const mainImage = document.getElementById('main_img' + listId);
    const imgId = document.getElementById('img' + listId);

    if (!mainImage || !imgId || !Array.isArray(imageArr) || imageArr.length <= 1
            || typeof initImageSwipe !== 'function') {
        return;
    }

    initImageSwipe(mainImage, {
        canPrevious: () => Number(imgId.value) > 0,
        canNext: () => Number(imgId.value) < imageArr.length - 1,
        getPreviousSource: () => imageArr[Number(imgId.value) - 1].imgSrc,
        getNextSource: () => imageArr[Number(imgId.value) + 1].imgSrc,
        onPrevious: () => fnLeft(listId, imageArr, true),
        onNext: () => fnRight(listId, imageArr, true),
    });
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
            like.src = liked ? '/img/ic_favorite_border.png' : '/img/ic_favorite.png';
        } else {
            alert('다시 시도하여주십시오');
        }
    })
    .catch(() => {
        alert('다시 시도하여주십시오.');
    });
}

function fnOnlyLike(listId) {
    const like = document.getElementById('like' + listId);
    const likeCnt = document.getElementById('like_cnt' + listId);
    if (!like || !likeCnt) return;

    fetch(`/api/favorite/like?id=${listId}`, {
        method: 'POST',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['result'] > 0) {
            likeCnt.innerText = String(parseInt(likeCnt.innerText, 10) + 1);
            like.src = '/img/ic_favorite.png';
        } else if (json['result'] === -10) {
            alert('다시 시도하여주십시오');
        }
    })
    .catch(() => {
        alert('다시 시도하여주십시오');
    });
}

function fnComment(listId) {
    if (typeof openCommentModal === 'function') {
        openCommentModal(listId);
        return;
    }

    location.href = '/comment?id=' + listId;
}

function fnHref(listId) {
    if (typeof openFavoriteModal === 'function') {
        openFavoriteModal(listId);
        return;
    }

    location.href = '/favorites?id=' + listId;
}

function fnReportBoard(listId) {
    location.href = `/report?targetType=BOARD&targetId=${encodeURIComponent(listId)}`;
}

function fnProfile(email) {
    location.href = '/member/search/detail?email=' + email;
}
