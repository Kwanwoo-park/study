const examine = document.getElementById("changeExamine");
const approval = document.getElementById("changeApproval")
const selectionCount = document.getElementById("forbidden-selection-count")

let idList = []

function fnCheck(id) {
    const checkBox = document.getElementById('check' + id)

    if (checkBox.checked)
        idList.push(id);
    else {
        for (let i = 0; i < idList.length; i++) {
            if (idList[i] == id) {
                idList.splice(i, 1);
                break;
            }
        }
    }

    updateActionState();
}

function updateActionState() {
    const selectedCount = idList.length;

    examine.disabled = selectedCount === 0;
    approval.disabled = selectedCount === 0;
    selectionCount.innerText = selectedCount === 0 ? '선택된 항목 없음' : `${selectedCount}개 항목 선택됨`;
    selectionCount.classList.toggle('has-selection', selectedCount > 0);
}

examine.addEventListener("click", (event) => {
    if (idList.length > 0){
        fetch(`/api/admin/forbidden/word/change/examine`, {
            method: 'PATCH',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            body: JSON.stringify({
                idList: idList
            }),
            credentials: "include",
        })
        .then((response) => response.json())
        .then((json) => {
            if (json['result'] > 0) {
                alert('변경 완료')
                window.location.reload();
            }
            else {
                alert("디시 시도하여 주십시오")
            }
        })
        .catch((error) => {
            console.error(error)
            alert("디시 시도하여 주십시오")
        })
    }
})

approval.addEventListener("click", (event) => {
    if (idList.length > 0) {
        fetch(`/api/admin/forbidden/word/change/approval`, {
            method: 'PATCH',
            headers: {
                "Content-Type": "application/json; charset-utf-8",
            },
            body: JSON.stringify({
                idList: idList
            }),
            credentials: "include",
        })
        .then((response) => response.json())
        .then((json) => {
            if (json['result'] > 0) {
                alert('변경 완료')
                window.location.reload();
            }
            else {
                alert("디시 시도하여 주십시오")
            }
        })
        .catch((error) => {
            console.error(error)
            alert("디시 시도하여 주십시오")
        })
    }
});
