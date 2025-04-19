const url = new URL(window.location.href)
const urlParams = url.searchParams

const submit = document.getElementById('submit');
const comments = document.getElementById('comments');

let flag = true;
let id;

function comment(e) {
    if (e.keyCode == 13 && !e.shiftKey) {
        event.preventDefault();

        submit.click();
    }
}

if (submit) {
    let apiUrl;
    let data;
    submit.addEventListener('click', (event) => {

        if (flag) {
            apiUrl = `/api/comment`

            data = {
                id: urlParams.get('id'),
                comments: comments.value
            }
        }
        else {
            apiUrl = `/api/reply`

            data = {
                id: id,
                reply: comments.value
            }
        }

        fetch(apiUrl, {
            method: 'POST',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            body: JSON.stringify(data),
        })
        .then((response) => response.json())
        .then((json) => {
            window.location.reload();
        })
        .catch((error) => {
            alert("다시 시도하여주십시오.");
        })
    })
}

function fnEdit(commentId) {
    const comment_edit = document.getElementById('edit_comment' + commentId);
    const comment = document.getElementById('comment' + commentId);

    if (comment_edit.style.display !== 'none')
    {
        const data = {
            id: commentId,
            comments: comment_edit.value
        }

        fetch(`/api/comment/update`, {
            method: 'PATCH',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            body: JSON.stringify(data),
        })
        .then((response) => response.json())
        .then((json) => {
            window.location.reload();
        })
        .catch((error) => {
            alert("다시 시도하여주십시오.");
        })
    }
    else {
        comment_edit.style.display = 'block';
        comment.style.display = 'none';
    }
}

function fnDelete(commentId) {
    const data = {
        id: commentId
    }

    fetch(`/api/comment/delete?id=` + urlParams.get('id'), {
        method: 'DELETE',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        body: JSON.stringify(data),
    })
    .then((response) => response.json())
    .then((json) => {
        alert("삭제가 완료되었습니다");
        window.location.reload();
    })
    .catch((error) => {
        alert("다시 시도하여주십시오");
    })
}

function fnReply(commentId, name) {
    flag = false;

    comments.value = '';
    comments.value += "@" + name + " ";
    comments.focus();

    id = commentId;
}

function fnReplyGet(commentId) {
    fetch(`/api/reply/list?id=` + commentId, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
    })
    .then((response) => response.json())
    .then((json) => {
        console.log(json);
    })
    .catch((error) => {
        alert("다시 시도하여주십시오");
    })
}