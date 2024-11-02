const button = document.getElementById('find');
const email = document.getElementById('email');

if (button) {
    button.addEventListener('click', (event) => {
        event.preventDefault();

        if (email.value != '') {
            fetch(`/api/member/find/email?email=`+email.value, {
                method: 'GET',
                headers: {
                    "Content-Type": "application/json; charset=utf-8",
                },
            })
            .then((response) => response.json())
            .then((json) => {
                location.href = "/member/updatePassword/" + json.email;
            })
            .catch((error) => {
                console.error(error)
                alert("존재하지 않는 회원정보입니다.");
            })
        }
    })
}

if (email) {
    email.addEventListener('keydown', (event) => {
        if (event.key == 'Enter')
            button.click();
    })
}