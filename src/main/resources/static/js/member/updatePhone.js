const phone = document.getElementById('phone')
const email = document.getElementById('email')
const birth = document.getElementById('birth')
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
                    phone: phone.value,
                    birth: birth.value,
                }),
                credentials: "include",
            })
            .then((response) => response.json())
            .then((json) => {
                if (json['result'] == -2)
                    alert("입력 값들을 확인해주세요")
                else if (json['result'] > 0)
                    alert("회원 정보 저장 완료되었습니다")
                else
                    alert("다시 시도하여주십시오");
            })
            .catch((error) => {
                console.error(error)
                alert("다시 시도하여주십시오");
            });
        }
    })
}