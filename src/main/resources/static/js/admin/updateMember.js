const role = document.getElementById('role')
const change = document.getElementById('change')
const del = document.getElementById('delete')
const id = document.getElementById('mid')

let url = `/api/admin/member`
let msg;

if (role.innerText == "USER") {
    change.innerText = "Access Deny";
    url += `/deny`;
    msg = "접근이 거부 되었습니다."
}
else if (role.innerText == "DENIED") {
    change.innerText = "Access Permit";
    url += `/permit`;
    msg = "접근이 허가 되었습니다."
}
else
    change.style.display = 'none';

if (change) {
    change.addEventListener('click', (event) => {
        fetch(url, {
            method: 'PATCH',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            body: JSON.stringify({
                id: id.value
            }),
        })
        .then((response) => response.json())
        .then((json) => {
            alert(msg);
            window.location.reload();
        })
        .catch((error) => {
            alert("다시 시도하여주십시오.");
        })
    })
}



