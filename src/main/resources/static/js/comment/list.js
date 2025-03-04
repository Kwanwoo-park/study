const url = new URL(window.location.href)
const urlParams = url.searchParams
const id = urlParams.get('id')

const submit = document.getElementById('submit');

if (submit) {
    submit.addEventListener('click', (event) => {
        const data = {
            comments: document.getElementById('comments').value
        }

        fetch(`/api/comment?id=` + id, {
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

    fetch(`/api/comment/delete?id=` + id, {
        method: 'DELETE',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        body: JSON.stringify(data),
    })
    .then((response) => response.json())
    .then((json) => {
        alert("삭제가 완료되었습니다.");
        window.location.reload();
    })
    .catch((error) => {
        alert("다시 시도하여주십시오.");
    })
}