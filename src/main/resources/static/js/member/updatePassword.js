const password = document.getElementById('password')
const email = document.getElementById('email')
const button = document.getElementById('update')

if (button) {
    button.addEventListener('click', (event) => {
        event.preventDefault();

        if (password != '') {
            fetch(`/api/member/updatePassword`, {
                method: 'PATCH',
                headers: {
                    "Content-Type": "application/json; charset=utf-8",
                },
                body: JSON.stringify({
                    email: email.value,
                    password: password.value
                }),
            })
            .then((response) => {
                if (response.status == 200) {
                    alert("비밀번호가 정상적으로 변경되었습니다");
                    location.replace(`/member/login`)
                }
                else {
                    console.error(response)
                    alert("다시 시도하여주십시오");
                }
            })
            .catch((error) => {
                console.error(error)
                alert("다시 시도하여주십시오");
            });
        }
    })
}

if (password) {
    password.addEventListener('keydown', (event) => {
        if (event.key == 'Enter')
            button.click();
    });
}