const examine = document.getElementById("changeExamine");
const approval = document.getElementById("changeApproval")

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
}

examine.addEventListener("click", (event) => {
    if (idList.length > 0){
        fetch(`/api/forbidden/word/admin/change/examine`, {
            method: 'PATCH',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            body: JSON.stringify({
                idList: idList
            }),
        })
        .then((response) => {
            if (response.status == 200) {
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
        fetch(`/api/forbidden/word/admin/change/approval`, {
            method: 'PATCH',
            headers: {
                "Content-Type": "application/json; charset-utf-8",
            },
            body: JSON.stringify({
                idList: idList
            }),
        })
        .then((response) => {
            if (response.status == 200) {
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