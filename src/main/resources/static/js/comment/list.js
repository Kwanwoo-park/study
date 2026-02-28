const url = new URL(window.location.href)
const urlParams = url.searchParams

const submit = document.getElementById('submit');
const comments = document.getElementById('comments');
const cancel = document.getElementById('cancel');

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
            credentials: "include",
        })
        .then((response) => response.json())
        .then((json) => {
            if (json['result'] == -1)
                alert("부적절한 내용 감지되었습니다");
            else if (json['result'] == -3) {
                alert("금칙어를 사용하여 계정이 정지되었습니다");
                window.location.reload();
            } else if (json['result'] == -10)
                alert("다시 시도하여주십시오");
            else
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

    if (comment_edit.style.display !== 'none') {
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
            credentials: "include",
        })
        .then((response) => response.json())
        .then((json) => {
            if (json['result'] == -1)
                alert("부적절한 내용 감지되었습니다");
            else if (json['result'] == -3) {
                alert("금칙어를 사용하여 계정이 정지되었습니다");
                window.location.reload();
            } else if (json['result'] == -10)
                alert("다시 시도하여주십시오");
            else
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
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['result'] > 0) {
            alert("삭제가 완료되었습니다");
            window.location.reload();
        } else
            alert("다시 시도하여주십시오");
    })
    .catch((error) => {
        alert("다시 시도하여주십시오");
    })
}

function fnReply(commentId, name) {
    flag = false;

    cancel.style.display = 'block';

    comments.value = '';
    comments.value += "@" + name + " ";
    comments.focus();

    id = commentId;
}

function fnCancel() {
    flag = true;

    id = null;

    comments.value = '';

    cancel.style.display = 'none';
}

function fnReplyGet(commentId) {
    const commentArea = document.getElementById('commentArea' + commentId);
    const replyArea = document.getElementById('replyArea' + commentId);

    if (!replyArea) {
        fetch(`/api/reply/list?id=` + commentId, {
            method: 'GET',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            credentials: "include",
        })
        .then((response) => response.json())
        .then((json) => {
            let area = document.createElement('div')
            area.id = 'replyArea' + commentId;

            commentArea.append(area)

            if (json['result'] > 0) {
                json.list.forEach(data => {
                    let newArea = document.createElement('span');
                    let div = document.createElement('div');
                    let replyDiv = document.createElement('div');
                    let profile = document.createElement('img');
                    let memberHref = document.createElement('a');
                    let commentHref = document.createElement('a');
                    let name = document.createElement('span');
                    let reply = document.createElement('pre');

                    profile.src = data['member'].profile;
                    profile.className = 'profile';

                    memberHref.href= "/member/search/detail?email=" + data['member'].email;
                    name.innerText = data['member'].name;

                    memberHref.append(name);

                    commentHref.href = "/member/search/detail?email=" + data['commentMember'].email;
                    commentHref.innerText = "@"+data['commentMember'].name;

                    reply.innerText = data['reply'];

                    replyDiv.append(commentHref)
                    replyDiv.append(reply)

                    replyDiv.className = 'replyDiv';

                    newArea.append(profile);
                    newArea.append(memberHref);
                    newArea.append(replyDiv);

                    newArea.className = 'newArea';

                    area.append(newArea);
                })
            } else
                alert("다시 시도하여주십시오");
        })
        .catch((error) => {
            console.error(error);
            alert("다시 시도하여주십시오");
        })
    }
    else {
        replyArea.remove();
    }
}