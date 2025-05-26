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
                if (json.role != "DENIED")
                    alert(json.name + "님 환영합니다!")

                if (json.role == "USER")
                    location.replace(`/board/main`)
                else if (json.role == "ADMIN")
                    location.replace(`/admin/administrator`)
                else
                    alert("Access Deny");
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