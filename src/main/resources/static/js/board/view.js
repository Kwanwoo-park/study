const boardDel = document.getElementById("boardDel");

function fnLeft(listId, ImageArr) {
    const main_image = document.getElementById('main_img' + listId);
    const img_id = document.getElementById('img' + listId);
    const left_arrow = document.getElementById('left' + listId);
    const right_arrow = document.getElementById('right' + listId);

    main_image.src = ImageArr[parseInt(img_id.value) - 1].imgSrc;
    img_id.value = parseInt(img_id.value) - 1;

     if (right_arrow.style.visibility === 'hidden')
        right_arrow.style.visibility = 'visible'

     if (parseInt(img_id.value) == 0)
        left_arrow.style.visibility = 'hidden'
}

function fnRight(listId, ImageArr) {
    const main_image = document.getElementById('main_img' + listId);
    const img_id = document.getElementById('img' + listId);
    const left_arrow = document.getElementById('left' + listId);
    const right_arrow = document.getElementById('right' + listId);

    main_image.src = ImageArr[parseInt(img_id.value)+ 1].imgSrc;
    img_id.value = parseInt(img_id.value)+ 1;

    if (left_arrow.style.visibility === 'hidden')
        left_arrow.style.visibility = 'visible'

    if (parseInt(img_id.value) == ImageArr.length-1)
        right_arrow.style.visibility = 'hidden'
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

function fnEdit() {
    const edit = document.getElementById('edit');
    const complete = document.getElementById('complete');
    const del = document.getElementById('delete');
    const content = document.getElementById('content');
    const editContent = document.getElementById('editContent');

    edit.style.display = 'none';
    del.style.display = 'none';
    content.style.display = 'none';

    complete.style.display = 'inline';
    editContent.style.display = 'inline';
}

function fnEditComplete(boardId) {
    const editContent = document.getElementById('editContent');

    const data = {
        id: boardId,
        content: editContent.value
    }

    fetch(`/api/board/view`, {
        method: 'PATCH',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        body: JSON.stringify(data),
    })
    .then((response) => response.json())
    .then((json) => {
        if (json == -1)
            alert("부적절한 내용 감지되었습니다");
        else
            window.location.reload();
    })
    .catch((error) => {
        alert("다시 시도하여주십시오");
    })
}

function fnDelete(boardId) {
    fetch(`/api/board/view/delete?id=` + boardId, {
        method: 'DELETE',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['status'] == 200)
            alert("삭제가 완료되었습니다");
        else
            alert("삭제 실패하였습니다");
    })
    .catch((error) => {
        alert("다시 시도하여주십시오");
    })
}

function fnImageDelete(boardId) {
    fetch(`/api/boardImg/delete?id=` + boardId, {
        method: 'DELETE',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
    })
    .then((response) => response.json())
    .then((json) => {
        status = json['status'];

        if (status == 200) {
            boardDel.click();
            location.replace(`/board/main`);
        }
        else
            alert("이미지 삭제 실패")
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