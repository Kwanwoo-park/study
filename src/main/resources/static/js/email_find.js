const button = document.getElementById('find');
const email = document.getElementById('email');

if (button) {
    button.addEventListener('click', (event) => {
        event.preventDefault();

        if (email.value != '') {
            fetch(`/api/member/find?email=`+email.value, {
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
                alert("다시 시도하여주십시오");
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