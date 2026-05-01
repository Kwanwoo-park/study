const container = document.querySelector('.container')

let nextCursor = 1;

window.onload = function() {
    if (typeof initCommentModal === 'function') {
        initCommentModal();
    }

    if (typeof initFavoriteModal === 'function') {
        initFavoriteModal();
    }

    loadMorePosts();
}

container.addEventListener('scroll', () => {
    if (container.scrollTop + container.clientHeight >= container.scrollHeight - 1) {
        loadMorePosts();
    }
})

async function loadMorePosts() {
    if (!nextCursor) return;

    try {
        const res = await fetch(`/api/board/load?cursor=` + (nextCursor-1) + `&limit=10`, {
            method: 'GET',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            credentials: "include",
        });
        const data = await res.json();

        if (data['result'] > 0) {
            fnDraw(data)

            nextCursor = data.nextCursor;

            if (!nextCursor){
                const load = document.createElement('span')
                load.id = 'loading'
                load.innerText = '모든 게시물을 불러왔습니다';
                container.append(load);
            }
        } else
            alert("다시 시도하여주십시오");
    } catch (e) {
        console.error('로드 오류', e);
    }
}

function fnDraw(data) {
    data.boards.forEach(board => {
        const main = document.createElement('div');
        main.className = 'main';

        const profile = document.createElement('div');
        profile.className = 'profile';
        profile.onclick = function() {
            fnProfile(board.member.email)
        };

        const pro_img = document.createElement('img');
        pro_img.className = 'profile-img';
        pro_img.src = board.member.profile;

        const name = document.createElement('span');
        name.className = 'name';
        name.innerText = board.member.name;

        profile.append(pro_img);
        profile.append(name);

        const main_image_div = document.createElement('div');
        main_image_div.className = 'main-image-wrapper'

        if (board.img.length != 1) {
            const button = document.createElement('button')
            button.className = 'arrow';
            button.type = 'button'
            button.id = 'left' + board.id;
            button.style.visibility = 'hidden';
            button.onclick = function() {
                fnLeft(board.id, board.img);
            }
            button.innerText = '←'
            main_image_div.append(button)
        }

        const main_img = document.createElement('img')
        main_img.className = 'main-image';
        main_img.id = 'main_img' + board.id;
        main_img.addEventListener('dblclick', () => {
            fnOnlyLike(board.id)
        })

        if (board.img.length == 0)
            main_img.src = "/img/IMG_0111.jpeg";
        else
            main_img.src = board.img[0].imgSrc;

        main_image_div.append(main_img);

        const img_idx = document.createElement('input')
        img_idx.type = 'hidden'
        img_idx.id = 'img' + board.id;
        img_idx.value = 0;

        main_image_div.append(img_idx)

        if (board.img.length > 1) {
            const button = document.createElement('button')
            button.className = 'arrow';
            button.type = 'button'
            button.id = 'right' + board.id;
            button.onclick = function() {
                fnRight(board.id, board.img);
            }
            button.innerText = '→'
            main_image_div.append(button);
        }

        const info = document.createElement('div');
        info.className = 'info'

        const icon_div = document.createElement('div')
        icon_div.className = 'icon-box'

        const img_like = document.createElement('img')
        img_like.className = 'icon';
        img_like.id = 'like' + board.id;
        img_like.onclick = function() {
            fnLike(board.id);
        }

        if (data.like[board.id])
            img_like.src = "/img/ic_favorite.png"
        else
            img_like.src = "/img/ic_favorite_border.png";

        const img_comment = document.createElement('img')
        img_comment.className = 'icon';
        img_comment.id = 'comment' + board.id;
        img_comment.onclick = function() {
            fnOpenComment(board.id);
        }
        img_comment.src = '/img/ic_chat_black.png';

        icon_div.append(img_like)
        icon_div.append(img_comment)

        const like_div = document.createElement('div')
        like_div.className = 'like'
        like_div.onclick = function() {
            fnOpenFavorite(board.id);
        }

        const label1 = document.createElement('label')
        label1.className = 'form-label'
        label1.innerText = '좋아요';

        const label2 = document.createElement('label')
        label2.className = 'form-label'
        label2.id = 'like_cnt' + board.id;
        label2.innerText = data.like_count[board.id];

        const label3 = document.createElement('label')
        label3.className = 'form-label'
        label3.innerText = '개';

        like_div.append(label1)
        like_div.append(label2)
        like_div.append(label3)

        info.append(icon_div)
        info.append(like_div)

        const name2 = document.createElement('span');
        name2.className = 'name';
        name2.innerText = board.member.name;

        info.append(name2)

        const pre = document.createElement('pre');
        pre.innerText = board.content;

        info.append(pre)

        const comment_div = document.createElement('div')
        comment_div.className = 'comment'
        comment_div.onclick = function() {
            fnOpenComment(board.id)
        }

        const label4 = document.createElement('label')
        label4.className = 'form-label'
        label4.innerText = '댓글';

        const label5 = document.createElement('label')
        label5.className = 'form-label'
        label5.id = 'comment_cnt' + board.id;
        label5.innerText = data.comment_count[board.id];

        const label6 = document.createElement('label')
        label6.className = 'form-label'
        label6.innerText = '개 모두 보기';

        comment_div.append(label4)
        comment_div.append(label5)
        comment_div.append(label6)

        info.append(comment_div)

        const date = new Date(board.registerTime)
        let temp = date.getFullYear() + "년 " +
                    (date.getMonth() + 1) + "월 " +
                    date.getDate() + "일"

        const date_label = document.createElement('label')
        date_label.className = 'date'
        date_label.innerText = temp;

        info.append(date_label)

        main.append(profile);
        main.append(main_image_div);
        main.append(info)

        container.appendChild(main);
    });
}

function fnOpenComment(boardId) {
    if (typeof openCommentModal === 'function') {
        openCommentModal(boardId);
        return;
    }

    fnComment(boardId);
}

function fnOpenFavorite(boardId) {
    if (typeof openFavoriteModal === 'function') {
        openFavoriteModal(boardId);
        return;
    }

    fnHref(boardId);
}