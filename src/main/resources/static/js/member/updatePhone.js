const phone = document.getElementById('phone')
const email = document.getElementById('email')
const button = document.getElementById('update')

if (button) {
    button.addEventListener('click', (event) => {
        event.preventDefault();

        if (phone != '') {
            fetch(`/api/member/updatePhone`, {
                method: 'PATCH',
                headers: {
                    "Content-Type": "application/json; charset=utf-8",
                },
                body: JSON.stringify({
                    email: email.value,
                    phone: phone.value
                }),
            })
            .then((response) => {
                if (response.status == 200) {
                    alert("전화번호가 정상적으로 저장되었습니다.");
                    location.replace(`/board/list`)
                }
                else {
                    alert("가입된 전화번호 입니다");
                }
            })
            .catch((error) => {
                console.error(error)
                alert("다시 시도하여주십시오");
            });
        }
    })
}