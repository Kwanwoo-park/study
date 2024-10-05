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
        .then((response) => {
            if (response.ok) {
                fetch(`/jwt/auth`, {
                    method: 'GET',
                    headers: {
                        "Content-Type": "application/json"
                    },
                })
                .then((response) => {
                    console.log(response)
                    if (response.ok) {
                        location.replace(`/board/list`)
                    }
                    else
                        alert("Authorization Fail")
                })
                .catch((error) => {
                    console.error('Error during login:', error);
                    alert('An error occurred. Please try again later');
                });
            } else {
                alert("Login Fail");
            }
         })
        .catch((error) => {
            console.error('Error during login:', error);
            alert('An error occurred. Please try again later');
        });
    });
}

if (password) {
    password.addEventListener('keyup', (event) => {
        if (event.key == 'Enter')
            button.click();
    });
}

