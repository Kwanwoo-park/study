function fnLeft(listId, imageArr) {
    if (!Array.isArray(imageArr) || imageArr.length === 0) return;

    const mainImage = document.getElementById('main_img' + listId);
    const imgId = document.getElementById('img' + listId);
    const leftArrow = document.getElementById('left' + listId);
    const rightArrow = document.getElementById('right' + listId);

    if (!mainImage || !imgId) return;

    const currentIndex = parseInt(imgId.value, 10);
    if (Number.isNaN(currentIndex) || currentIndex <= 0) return;

    const nextIndex = currentIndex - 1;
    mainImage.src = imageArr[nextIndex].imgSrc;
    imgId.value = nextIndex;

    if (rightArrow && rightArrow.style.visibility === 'hidden') {
        rightArrow.style.visibility = 'visible';
    }

    if (leftArrow && nextIndex === 0) {
        leftArrow.style.visibility = 'hidden';
    }
}

function fnRight(listId, imageArr) {
    if (!Array.isArray(imageArr) || imageArr.length === 0) return;

    const mainImage = document.getElementById('main_img' + listId);
    const imgId = document.getElementById('img' + listId);
    const leftArrow = document.getElementById('left' + listId);
    const rightArrow = document.getElementById('right' + listId);

    if (!mainImage || !imgId) return;

    const currentIndex = parseInt(imgId.value, 10);
    if (Number.isNaN(currentIndex) || currentIndex >= imageArr.length - 1) return;

    const nextIndex = currentIndex + 1;
    mainImage.src = imageArr[nextIndex].imgSrc;
    imgId.value = nextIndex;

    if (leftArrow && leftArrow.style.visibility === 'hidden') {
        leftArrow.style.visibility = 'visible';
    }

    if (rightArrow && nextIndex === imageArr.length - 1) {
        rightArrow.style.visibility = 'hidden';
    }
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

function fnProfile(email) {
    location.href = '/member/search/detail?email=' + email;
}
