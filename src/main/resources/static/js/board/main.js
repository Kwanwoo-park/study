const container = document.querySelector('.container')

let nextCursor = 1;
let loading = false;

window.onload = function() {
    initCommentModal();
    initFavoriteModal();
    loadMorePosts();
}

container.addEventListener('scroll', () => {
    if (container.scrollTop + container.clientHeight >= container.scrollHeight - 1) {
        loadMorePosts();
    }
})

async function loadMorePosts() {
    if (loading || !nextCursor) return;
    loading = true;
    try {
        const res = await fetch(`/api/board/load?cursor=` + (nextCursor-1) + `&limit=10`, {
            credentials: "include",
        });
        const data = await res.json();

        if (data['result'] > 0) {
            renderPosts(data);

            nextCursor = data.nextCursor;

            if (!nextCursor){
                if (!document.getElementById('loading')) {
                    const load = document.createElement('span')
                    load.id = 'loading'
                    load.innerText = '모든 게시물을 불러왔습니다';
                    container.append(load);
                }
            }
        } else
            alert("다시 시도하여주십시오");
    } catch (e) {
        console.error('로드 오류', e);
    } finally {
        loading = false;
    }
}

function renderPosts(data) {
    for (const board of data.boards) {
        const post = postElement('div', 'main');
        const content = postElement('pre', '', board.content);
        post.append(createPostProfile(board.member), createPostGallery(board), createPostInfo(board, data, content));
        container.append(post);
        initBoardContent(content);
        enableBoardImageSwipe(board.id, board.img);
    }
}

function createPostProfile(member) {
    const profile = postElement('div', 'profile');
    const image = postElement('img', 'profile-img');
    image.src = member.profile;
    profile.onclick = () => fnProfile(member.email);
    profile.append(image, postElement('span', 'name', member.name));
    return profile;
}

function createPostGallery(board) {
    const gallery = postElement('div', 'main-image-wrapper');
    const image = postElement('img', 'main-image');
    image.id = 'main_img' + board.id;
    image.src = board.img.length ? board.img[0].imgSrc : '/img/IMG_0111.jpeg';
    image.addEventListener('dblclick', () => fnOnlyLike(board.id));

    const index = document.createElement('input');
    index.type = 'hidden';
    index.id = 'img' + board.id;
    index.value = 0;
    gallery.append(image, index);

    if (board.img.length > 1) {
        const previous = galleryArrow('left' + board.id, '‹', () => fnLeft(board.id, board.img));
        previous.classList.add('is-invisible');
        const next = galleryArrow('right' + board.id, '›', () => fnRight(board.id, board.img));
        const counter = postElement('span', 'image-counter', `1 / ${board.img.length}`);
        counter.id = 'imageCounter' + board.id;
        gallery.prepend(previous);
        gallery.append(next, counter);
    }

    preloadAdjacentImages(board.img, 0);
    return gallery;
}

function galleryArrow(id, text, onClick) {
    const button = postElement('button', 'arrow', text);
    button.type = 'button';
    button.id = id;
    button.onclick = onClick;
    return button;
}

function createPostInfo(board, data, content) {
    const id = board.id;
    const info = postElement('div', 'info');
    const date = new Date(board.registerTime);
    const registration = `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일`;

    info.append(
        createPostActions(board, data.email, data.like[id]),
        createPostCount('like', '좋아요', 'like_cnt' + id, data.like_count[id], '개', () => fnHref(id)),
        postElement('span', 'name', board.member.name),
        content,
        createPostCount('comment', '댓글', 'comment_cnt' + id, data.comment_count[id], '개 모두 보기', () => fnComment(id)),
        postElement('label', 'date', registration)
    );
    return info;
}

function createPostActions(board, viewerEmail, liked) {
    const actions = postElement('div', 'icon-box');
    const like = postElement('img', 'icon');
    like.id = 'like' + board.id;
    like.src = liked ? '/img/ic_favorite.png' : '/img/ic_favorite_border.png';
    like.onclick = () => fnLike(board.id);

    const comment = postElement('img', 'icon');
    comment.id = 'comment' + board.id;
    comment.src = '/img/ic_chat_black.png';
    comment.onclick = () => fnComment(board.id);
    actions.append(like, comment);

    if (board.member.email !== viewerEmail) {
        const report = postElement('button', 'btn btn-outline-danger btn-sm report-button', '신고');
        report.type = 'button';
        report.onclick = () => fnReportBoard(board.id);
        actions.append(report);
    }
    return actions;
}

function createPostCount(className, label, id, count, suffix, onClick) {
    const row = postElement('div', className);
    const total = postElement('label', 'form-label', count);
    total.id = id;
    row.onclick = onClick;
    row.append(postElement('label', 'form-label', label), total, postElement('label', 'form-label', suffix));
    return row;
}

function postElement(tag, className, text = '') {
    const element = document.createElement(tag);
    element.className = className;
    element.textContent = text;
    return element;
}
