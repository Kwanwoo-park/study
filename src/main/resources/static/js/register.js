const email = document.getElementById('email');
const password = document.getElementById('password');
const name = document.getElementById('name');
const phone = document.getElementById('phone');
const birth = document.getElementById('birth');
const button = document.getElementById('submit');

if (button) {
    button.addEventListener('click', (event) => {
        event.preventDefault();

        if (email.value != '' && password.value != '' && name.value != '' && phone.value != '' && birth.value != '') {
            fetch(`/api/member/register`, {
                method: 'POST',
                headers: {
                    "Content-Type": "application/json; charset=utf-8",
                },
                body: JSON.stringify({
                    email: email.value,
                    password: password.value,
                    name: name.value,
                    phone: phone.value,
                    birth: birth.value
                }),
            })
            .then((response) => response.json())
            .then((json) => {
                alert("회원가입이 완료되었습니다.");
                location.replace(`/member/login`)
            })
            .catch((error) => {
                alert("이미 회원가입이 된 이메일입니다.");
            });
        }
    })
}