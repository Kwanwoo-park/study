const button = document.getElementById('login');
const password = document.getElementById('password');

if (button) {
    button.addEventListener('click', (event) => {
        event.preventDefault();

        fetch(`/member/login/action`, {
            method: 'POST',
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                email: document.getElementById('email').value,
                password: document.getElementById('password').value,
            }),
        })
        .then((response) => response.json())
        .then((json) => {
            alert(json.name + "님 환영합니다!")

            if (json.role == "USER")
                location.replace(`/board/list`)
            else
                location.replace(`/admin/administrator`)
        })
        .catch((error) => {
            console.error('Error during login:', error);
            alert('Login error occurred. Please try again later');
        });
    });
}

if (password) {
    password.addEventListener('keyup', (event) => {
        if (event.key == 'Enter')
            button.click();
    });
}