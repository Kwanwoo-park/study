async function loadMorePosts() {
    if (loading || !nextCursor) return;
    loading = true;
    document.getElementById('loading').innerText = "불러오는 중...";

    try {
        const res = await fetch(`/board/load?cursor=` + nextCursor + `&limit=10`);
        const data = await res.json();

        const container = document.getElementById('post-container');

        data.boards.forEach(board => {
            const div = document.createElement('div');
            div.className = 'board';
            div.innerHTML = `
                <p>${board.content}</p>
                <small>${board.registerTime.replace('T', ' ').split('.')[0]}</small>
            `;
            container.appendChild(div);
        });

        nextCursor = data.nextCursor;

        if (!nextCursor)
            document.getElementById('loading').innerText = '모든 게시물을 불러왔습니다';
        else
            document.getElementById('loading').innerText = '';
    } catch (e) {
        console.error('로드 오류', e);
    } finally {
        loading = false;
    }
}

window.addEventListener('scroll', () => {
    if (window.innerHeight + window.scrollY >= document.body.offsetHeight - 200) {
        loadMorePosts();
    }
})

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
            credentials: "include",
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
            credentials: "include",
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
        credentials: "include",
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