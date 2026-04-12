const url = new URL(window.location.href);
const urlParams = url.searchParams;

const submit = document.getElementById('submit');
const comments = document.getElementById('comments');
const cancel = document.getElementById('cancel');
const container = document.querySelector('.container');
const commentList = document.getElementById('commentList');

const COMMENT_LIMIT = 10;
const REPLY_LIMIT = 10;

let flag = true;
let id;
let nextCommentCursor = 1;
let isCommentLoading = false;

function comment(e) {
    if (e.keyCode == 13 && !e.shiftKey) {
        e.preventDefault();
        submit.click();
    }
}

window.onload = function() {
    loadMoreComments(true);
};

if (container) {
    container.addEventListener('scroll', () => {
        if (container.scrollTop + container.clientHeight >= container.scrollHeight - 10) {
            loadMoreComments();
        }
    });
}

if (submit) {
    let apiUrl;
    let data;
    submit.addEventListener('click', () => {
        if (flag) {
            apiUrl = '/api/comment';
            data = {
                id: urlParams.get('id'),
                comments: comments.value
            };
        } else {
            apiUrl = '/api/reply';
            data = {
                id: id,
                reply: comments.value
            };
        }

        fetch(apiUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
            },
            body: JSON.stringify(data),
            credentials: 'include',
        })
            .then((response) => response.json())
            .then((json) => {
                if (json.result == -1) {
                    alert('부적절한 내용 감지되었습니다');
                } else if (json.result == -3) {
                    alert('금칙어를 사용하여 계정이 정지되었습니다');
                    window.location.reload();
                } else if (json.result == -10) {
                    alert('다시 시도하여주십시오');
                } else if (flag) {
                    comments.value = '';
                    loadMoreComments(true);
                } else {
                    const replyCommentId = id;
                    fnCancel();
                    const replyState = getReplyState(replyCommentId);
                    replyState.nextCursor = 1;
                    replyState.items = [];
                    const replyArea = document.getElementById('replyArea' + replyCommentId);
                    if (replyArea) {
                        replyArea.remove();
                    }
                    fnReplyGet(replyCommentId);
                }
            })
            .catch(() => {
                alert('다시 시도하여주십시오.');
            });
    });
}

function fnEdit(commentId) {
    const commentEdit = document.getElementById('edit_comment' + commentId);
    const comment = document.getElementById('comment' + commentId);

    if (commentEdit.style.display !== 'none') {
        const data = {
            id: commentId,
            comments: commentEdit.value
        };

        fetch('/api/comment/update', {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
            },
            body: JSON.stringify(data),
            credentials: 'include',
        })
            .then((response) => response.json())
            .then((json) => {
                if (json.result == -1) {
                    alert('부적절한 내용 감지되었습니다');
                } else if (json.result == -3) {
                    alert('금칙어를 사용하여 계정이 정지되었습니다');
                    window.location.reload();
                } else if (json.result == -10) {
                    alert('다시 시도하여주십시오');
                } else {
                    loadMoreComments(true);
                }
            })
            .catch(() => {
                alert('다시 시도하여주십시오.');
            });
    } else {
        commentEdit.style.display = 'block';
        comment.style.display = 'none';
    }
}

function fnDelete(commentId) {
    const data = { id: commentId };

    fetch('/api/comment/delete?id=' + urlParams.get('id'), {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json; charset=utf-8',
        },
        body: JSON.stringify(data),
        credentials: 'include',
    })
        .then((response) => response.json())
        .then((json) => {
            if (json.result > 0) {
                loadMoreComments(true);
            } else {
                alert('다시 시도하여주십시오');
            }
        })
        .catch(() => {
            alert('다시 시도하여주십시오');
        });
}

function fnReply(commentId, name) {
    flag = false;
    cancel.style.display = 'block';
    comments.value = '@' + name + ' ';
    comments.focus();
    id = commentId;
}

function fnCancel() {
    flag = true;
    id = null;
    comments.value = '';
    cancel.style.display = 'none';
}

async function loadMoreComments(reset = false) {
    if (!commentList || isCommentLoading) {
        return;
    }

    if (reset) {
        nextCommentCursor = 1;
        commentList.innerHTML = '';
    }

    if (!nextCommentCursor) {
        return;
    }

    isCommentLoading = true;

    try {
        const response = await fetch(`/api/comment/list?id=${urlParams.get('id')}&cursor=${nextCommentCursor - 1}&limit=${COMMENT_LIMIT}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
            },
            credentials: 'include',
        });
        const json = await response.json();

        if (json.result < 0) {
            alert('다시 시도하여주십시오');
            return;
        }

        renderComments(json, reset);
        nextCommentCursor = json.nextCursor;
    } catch (error) {
        console.error(error);
        alert('다시 시도하여주십시오');
    } finally {
        isCommentLoading = false;
    }
}

function renderComments(json, reset) {
    if (!commentList) {
        return;
    }

    if (reset) {
        commentList.innerHTML = '';
    }

    if (json.list.length === 0 && commentList.children.length === 0) {
        const empty = document.createElement('li');
        empty.className = 'list-group-item';
        empty.innerText = '등록된 댓글이 없습니다';
        commentList.append(empty);
        return;
    }

    removeCommentLoadMore();

    json.list.forEach((item) => {
        const li = document.createElement('li');
        li.className = 'list-group-item';
        li.innerHTML = buildCommentItem(item, json.member);
        commentList.append(li);
    });

    if (json.nextCursor) {
        const more = document.createElement('li');
        more.className = 'list-group-item';
        more.id = 'commentLoadMore';

        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'btn btn-light';
        button.innerText = '댓글 더 보기';
        button.onclick = () => loadMoreComments();

        more.append(button);
        commentList.append(more);
    }
}

function buildCommentItem(item, memberEmail) {
    return `
        <span id="commentArea${item.id}">
            <img src="${escapeHtml(item.member.profile)}" class="profile" alt="profile">
            <a href="/member/search/detail?email=${encodeURIComponent(item.member.email)}">
                <span>${escapeHtml(item.member.name)}</span>
            </a>
            ${item.member.email === memberEmail ? `
                <div class="float-right">
                    <button type="button" class="btn btn-primary" onclick="fnEdit(${item.id})">Edit</button>
                    <button type="button" class="btn btn-danger" onclick="fnDelete(${item.id})">Delete</button>
                </div>
            ` : ''}
            <pre id="comment${item.id}">${escapeHtml(item.comments)}</pre>
            <textarea class="form-control" id="edit_comment${item.id}" rows="5" name="Comment edit" style="display: none;">${escapeHtml(item.comments)}</textarea>
            <label class="reply" onclick="fnReply(${item.id}, '${escapeJsString(item.member.name)}')">답글 달기</label>
            ${item.replyCount > 0 ? `
                <div class="reply" onclick="fnReplyGet(${item.id})" id="replyToggle${item.id}">
                    <label class="form-label">답글 </label>
                    <label class="form-label" id="reply_cnt${item.id}">${item.replyCount}</label>
                    <label class="form-label">개 모두 보기</label>
                </div>
            ` : ''}
        </span>
    `;
}

const replyStateMap = new Map();

function getReplyState(commentId) {
    if (!replyStateMap.has(commentId)) {
        replyStateMap.set(commentId, {
            nextCursor: 1,
            isLoading: false,
            items: []
        });
    }

    return replyStateMap.get(commentId);
}

async function fnReplyGet(commentId) {
    const commentArea = document.getElementById('commentArea' + commentId);
    const replyArea = document.getElementById('replyArea' + commentId);
    const state = getReplyState(commentId);

    if (replyArea) {
        replyArea.remove();
        return;
    }

    const area = document.createElement('div');
    area.id = 'replyArea' + commentId;
    commentArea.append(area);

    if (state.items.length === 0) {
        await loadReplies(commentId, true);
        return;
    }

    renderReplies(commentId);
}

async function loadReplies(commentId, reset = false) {
    const state = getReplyState(commentId);
    if (state.isLoading) {
        return;
    }

    if (reset) {
        state.nextCursor = 1;
        state.items = [];
    }

    if (!state.nextCursor) {
        renderReplies(commentId);
        return;
    }

    state.isLoading = true;

    try {
        const response = await fetch(`/api/reply/list?id=${commentId}&cursor=${state.nextCursor - 1}&limit=${REPLY_LIMIT}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
            },
            credentials: 'include',
        });
        const json = await response.json();

        if (json.result <= 0) {
            alert('다시 시도하여주십시오');
            return;
        }

        state.items = reset ? json.list.slice() : state.items.concat(json.list);
        state.nextCursor = json.nextCursor;
        renderReplies(commentId);
    } catch (error) {
        console.error(error);
        alert('다시 시도하여주십시오');
    } finally {
        state.isLoading = false;
    }
}

function renderReplies(commentId) {
    const area = document.getElementById('replyArea' + commentId);
    const state = getReplyState(commentId);
    if (!area) {
        return;
    }

    area.innerHTML = '';

    state.items.forEach((data) => {
        const newArea = document.createElement('span');
        const replyDiv = document.createElement('div');
        const profile = document.createElement('img');
        const memberHref = document.createElement('a');
        const commentHref = document.createElement('a');
        const name = document.createElement('span');
        const reply = document.createElement('pre');

        profile.src = data.member.profile;
        profile.className = 'profile';

        memberHref.href = '/member/search/detail?email=' + encodeURIComponent(data.member.email);
        name.innerText = data.member.name;
        memberHref.append(name);

        commentHref.href = '/member/search/detail?email=' + encodeURIComponent(data.commentMember.email);
        commentHref.innerText = '@' + data.commentMember.name;

        reply.innerText = data.reply;

        replyDiv.append(commentHref);
        replyDiv.append(reply);
        replyDiv.className = 'replyDiv';

        newArea.append(profile);
        newArea.append(memberHref);
        newArea.append(replyDiv);
        newArea.className = 'newArea';

        area.append(newArea);
    });

    if (state.nextCursor) {
        const more = document.createElement('button');
        more.type = 'button';
        more.className = 'btn btn-light btn-sm';
        more.innerText = '답글 더 보기';
        more.onclick = () => loadReplies(commentId);
        area.append(more);
    }
}

function removeCommentLoadMore() {
    const loadMore = document.getElementById('commentLoadMore');
    if (loadMore) {
        loadMore.remove();
    }
}

function escapeHtml(value) {
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

function escapeJsString(value) {
    return String(value)
        .replaceAll('\\', '\\\\')
        .replaceAll("'", "\\'");
}
