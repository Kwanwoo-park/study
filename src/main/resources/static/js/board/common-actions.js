function fnLeft(listId, imageArr, skipAnimation) {
    moveBoardImage(listId, imageArr, -1, skipAnimation);
}

function fnRight(listId, imageArr, skipAnimation) {
    moveBoardImage(listId, imageArr, 1, skipAnimation);
}

function moveBoardImage(listId, imageArr, direction, skipAnimation) {
    if (imageArr.length < 2) return;
    const mainImage = document.getElementById('main_img' + listId);
    const imgId = document.getElementById('img' + listId);
    const nextIndex = Number(imgId.value) + direction;
    if (nextIndex < 0 || nextIndex >= imageArr.length) return;
    if (!skipAnimation && animateImageSwipe(mainImage, direction < 0 ? 'previous' : 'next')) return;
    mainImage.src = imageArr[nextIndex].imgSrc;
    imgId.value = nextIndex;
    document.getElementById('imageCounter' + listId).innerText = `${nextIndex + 1} / ${imageArr.length}`;
    document.getElementById('left' + listId).classList.toggle('is-invisible', nextIndex === 0);
    document.getElementById('right' + listId).classList.toggle('is-invisible', nextIndex === imageArr.length - 1);
    preloadAdjacentImages(imageArr, nextIndex);
}

function enableBoardImageSwipe(listId, imageArr) {
    if (imageArr.length < 2) return;
    const mainImage = document.getElementById('main_img' + listId);
    const imgId = document.getElementById('img' + listId);

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
    openCommentModal(listId);
}

function fnHref(listId) {
    openFavoriteModal(listId);
}

function fnReportBoard(listId) {
    window.location.href = `/report?targetType=BOARD&targetId=${encodeURIComponent(listId)}`;
}

function fnProfile(email) {
    window.location.href = '/member/search/detail?email=' + email;
}
