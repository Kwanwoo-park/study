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
        .then((response) => response.json())
        .then((json) => {
             like_cnt.innerText = parseInt(like_cnt.innerText) + 1
             like.src = "/img/" + "ic_favorite.png"
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
        .then((response) => response.json())
        .then((json) => {
            like_cnt.innerText = parseInt(like_cnt.innerText) - 1
            like.src = "/img/" + "ic_favorite_border.png"
        })
        .catch((error) => {
            alert("다시 시도하여주십시오.");
        })
    }
}

function fnImg(listId, list, member) {
    const like = document.getElementById('like' + listId)
    var flag = false

    for (var i = 0; i < list.length; i++) {
        for (var j = 0; j < member.length; j++) {
            if (list[i].id == member[j].id) {
                flag = true;
                break;
            }
        }
    }

    if (flag) like.src = "/img/" + "ic_favorite.png"
    else like.src = "/img/" + "ic_favorite_border.png"
}
