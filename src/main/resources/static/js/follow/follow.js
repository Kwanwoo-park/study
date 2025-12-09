function fnFollow(listId, member_email) {
    const btn = document.getElementById('follow' + listId);
    let method;

    if (btn.innerText == 'Follow') {
        method = 'POST';
    }
    else if (btn.innerText == 'Unfollow') {
        method = 'DELETE';
    }

    const data = {
        email: member_email
    }

    fetch(`/api/follow`, {
        method: method,
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        body: JSON.stringify(data),
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['result'] > 0)
            window.location.reload();
        else
            alert("다시 시도하여주십시오");
    })
    .catch((error) => {
        alert("다시 시도하여주십시오")
    })
}