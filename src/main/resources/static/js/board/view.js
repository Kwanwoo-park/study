const id = document.getElementById('id').value;
const member = document.getElementById('email').value;
const role = document.getElementById('role').value;
const board = document.getElementById('board').value;

const delete_btn = document.getElementById('delete');
const edit = document.getElementById('edit');
const submit = document.getElementById('submit');

if (role != "ADMIN") {
    if (member != board) {
        document.getElementById('title').disabled = true;
        document.getElementById('content').disabled = true;
        edit.style.display = 'none';
        delete_btn.style.display = 'none';
    }
}

if (delete_btn) {
    delete_btn.addEventListener('click', (event) => {
        if (confirm("게시글을 삭제하시겠습니까?")) {
            fetch(`/api/board/view/delete?id=` + id, {
                method: 'DELETE',
                headers: {
                    "Content-Type": "application/json; charset=utf-8",
                },
            })
            .then((response) => response.json())
            .then((json) => {
                alert("삭제가 완료되었습니다.");
                location.replace(`/board/list`);
            })
            .catch((error) => {
                alert("삭제에 실패했습니다.");
            })
        }
    })
}

if (edit) {
    edit.addEventListener('click', (event) => {
        const data = {
            id: id,
            title: document.getElementById('title').value,
            content: document.getElementById('content').value
        }

        fetch(`/api/board/view?id=` + id, {
            method: 'PATCH',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            body: JSON.stringify(data),
        })
        .then((response) => response.json())
        .then((json) => {
            alert("게시글이 수정되었습니다.");
            window.location.reload();
        })
        .catch((error) => {
            alert("다시 시도하여주십시오.");
        })
    })
}

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