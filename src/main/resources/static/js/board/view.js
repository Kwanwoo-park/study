function fnLeft(listId, ImageArr) {
    const main_image = document.getElementById('main_img' + listId);
    const img_id = document.getElementById('img' + listId);
    const left_arrow = document.getElementById('left' + listId);
    const right_arrow = document.getElementById('right' + listId);

    main_image.src = "/img/" + ImageArr[parseInt(img_id.value) - 1].imgSrc;
    img_id.value = parseInt(img_id.value) - 1;

    if (right_arrow.style.display === 'none')
        right_arrow.style.display = 'flex'

    if (parseInt(img_id.value) == 0)
        left_arrow.style.display = 'none'
}

function fnRight(listId, ImageArr) {
    const main_image = document.getElementById('main_img' + listId);
    const img_id = document.getElementById('img' + listId);
    const left_arrow = document.getElementById('left' + listId);
    const right_arrow = document.getElementById('right' + listId);

    main_image.src = "/img/" + ImageArr[parseInt(img_id.value)+ 1].imgSrc;
    img_id.value = parseInt(img_id.value)+ 1;

    if (left_arrow.style.display === 'none')
        left_arrow.style.display = 'flex'

    if (parseInt(img_id.value) == ImageArr.length-1)
        right_arrow.style.display = 'none'
}

function fnLike(listId) {
    const like = document.getElementById('like' + listId);
    const like_cnt = document.getElementById('like_cnt' + listId);

    var arr = like.src.split('/')

    if (arr[arr.length-1] == 'ic_favorite_border.png') {
        fetch(`/api/favorite/like?id=` + listId, {
            method: 'POST',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
        })
        .then((response) => {
            if (response.status == 200) {
                like_cnt.innerText = parseInt(like_cnt.innerText) + 1
                like.src = "/img/" + "ic_favorite.png"
            }
        })
        .catch((error) => {
            alert("다시 시도하여주십시오.");
        })
    }
    else if (arr[arr.length-1] == 'ic_favorite.png') {
        fetch(`/api/favorite/delete?id=` + listId, {
            method: 'DELETE',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
        })
        .then((response) => {
            if (response.status == 200) {
                like_cnt.innerText = parseInt(like_cnt.innerText) - 1
                like.src = "/img/" + "ic_favorite_border.png"
            }
        })
        .catch((error) => {
            alert("다시 시도하여주십시오.");
        })
    }
}

function fnOnlyLike(listId) {
    const like = document.getElementById('like' + listId);
    const like_cnt = document.getElementById('like_cnt' + listId);

    fetch(`/api/favorite/like?id=` + listId, {
        method: 'POST',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
    })
    .then((response) => {
        if (response.status == 200) {
            like_cnt.innerText = parseInt(like_cnt.innerText) + 1
             like.src = "/img/" + "ic_favorite.png"
        }
    })
    .catch((error) => {
        alert("다시 시도하여주십시오");
    })
}

function fnComment(listId) {
    location.href = "/comment?id=" + listId;
}

function fnHref(listId) {
    location.href = "/favorites?id=" + listId;
}

function fnProfile(email) {
    location.href = "/member/search/detail?email=" + email;
}

function fnPrevious(previous) {
    location.replace(`/board/view?id=` + previous)
}

function fnNext(next) {
    location.replace(`/board/view?id=` + next)
}