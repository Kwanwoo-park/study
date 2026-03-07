const boardDel = document.getElementById("boardDel");

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
        alert("다시 시도하여주십시오");
    })
}

function fnDelete(boardId) {
    fetch(`/api/board/view/delete?id=` + boardId, {
        method: 'DELETE',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['result'] != -1) {
            alert("삭제가 완료되었습니다");
            location.replace(`/member/detail?email=`+json['email']);
        }
        else
            alert("다시 시도하여주십시오");
    })
    .catch((error) => {
        alert("다시 시도하여주십시오");
    })
}

function fnPrevious(previous) {
    location.replace(`/board/view?id=` + previous)
}

function fnNext(next) {
    location.replace(`/board/view?id=` + next)
}
