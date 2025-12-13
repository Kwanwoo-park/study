const button = document.getElementById('login');
const email = document.getElementById('email');
const password = document.getElementById('password');

if (button) {
    button.addEventListener('click', (event) => {
        event.preventDefault();

        if (email.value != '' && password.value != '') {
            fetch(`/api/member/login`, {
                method: 'PATCH',
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    email: email.value,
                    password: password.value,
                }),
                credentials: "include",
            })
            .then((response) => response.json())
            .then((json) => {
                if (json['result'] > 0) {
                    alert(json.member.name + "님 환영합니다!");
                    location.replace(`/board/main`)
                } else if (json['result'] == -10) {
                    console.error('Error during login:', error);
                    alert('Login error occurred. Please try again later');
                }
                else if (json['result'] == -2)
                    alert("Access Deny");
                else if (json['result'] == -3)
                    alert("이메일을 확인해주세요")
                else
                    alert("비밀번호를 확인해주세요")
            })
            .catch((error) => {
                console.error('Error during login:', error);
                alert('Login error occurred. Please try again later');
            });
        }
    });
}

if (password) {
    password.addEventListener('keydown', (event) => {
        if (event.key == 'Enter')
            button.click();
    });
}