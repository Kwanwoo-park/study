const email = document.getElementById('email');
const email_check = document.getElementById('email_check');
const password = document.getElementById('password');
const name = document.getElementById('name');
const phone = document.getElementById('phone');
const birth = document.getElementById('birth');
const button = document.getElementById('submit');

var flag = false;

if (email_check) {
    email_check.addEventListener('click', (event) => {
        event.preventDefault();

        if (email.value != '') {
            fetch(`/api/member/duplicateCheck?email=` + email.value, {
                method: 'GET',
                headers: {
                    "Content-Type": "application/json; charset-utf-8",
                },
            })
            .then((response) => {
                if (response.status == 200) {
                    alert("사용 가능한 이메일입니다");
                    email.disabled = true;
                    button.style.display = 'inline';
                    flag = true;
                }
                else {
                    alert("이미 가입된 이메일입니다");
                }
            })
            .catch((error) => {
                console.error(error)
                alert("다시 시도하여주십시오")
            })
        }
    })
}

if (button) {
    button.addEventListener('click', (event) => {
        event.preventDefault();

        if (flag && password.value != '' && name.value != '' && phone.value != '' && birth.value != '') {
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
        else {
            alert("빈 칸 없이 모두 채워주세요");
        }
    })
}