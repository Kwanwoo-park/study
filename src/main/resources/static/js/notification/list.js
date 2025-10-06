function fnClick(id) {
    const button = document.getElementById(id);

    fetch(`/api/notification/mark-as-read?id=` + id, {
        method: 'PATCH',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => {
        if (response.status == 200) {
            alert('알림이 읽음으로 표시되었습니다');
            button.innerText = '읽음';
            button.style.disabled = false;
        }
        else {
            alert('알림 상태를 변경하는 중 오류가 발생했습니다');
        }
    })
    .catch((error) => {
        console.error(error);
        alert("다시 시도하여주십시오");
    });
}

function fnAllRead() {
    fetch(`/api/notification/mark-all-as-read`, {
        method: 'PATCH',
        headers: {
            "Content-Type": "appllication/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => {
        console.log(response);
        if (response.status == 200) {
            alert('알림이 모두 읽음으로 표시되었습니다');
            window.location.reload();
        } else {
            alert('알림 상태를 변경하는 중 오류가 발생했습니다');
        }
    })
    .catch((error) => {
        console.error(error);
        alert("다시 시도하여주십시오");
    })
}