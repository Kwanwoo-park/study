const button = document.getElementById('find');
const email = document.getElementById('email');
const check = document.getElementById('check');
const email_certification = document.getElementById('email_certification');
const memailconfirmTxt = document.getElementById('memailconfirmTxt');
const memailconfirm = document.getElementById('memailconfirm');

var code;

if (email_certification) {
    email_certification.addEventListener('click', (event) => {
        event.preventDefault();

        if (email.value != '') {
            fetch(`/api/mail/confirm`, {
                method: 'POST',
                headers: {
                    "Content-Type": "application/json; charset=utf-8",
                },
                body: JSON.stringify({
                    email: email.value
                }),
            })
            .then((response) => response.json())
            .then((json) => {
                code = json['code']
                alert("해당 이메일로 인증번호 발송이 완료되었습니다\n 확인부탁드립니다");
                check.style.display = 'inline';
                email.disabled = true;
            })
            .catch((error) => {
                alert("인증번호 발송에 실패하였습니다\n 다시 시도하여 주십시오");
            })
        }
        else {
            alert("이메일을 입력하여 주십시오");
        }
    })
}

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

if (memailconfirm) {
    memailconfirm.addEventListener('keyup', function() {
        if (code != memailconfirm.value) {
            memailconfirmTxt.innerText = '인증번호가 잘못되었습니다.';
        } else {
            button.style.display = 'inline';
            memailconfirmTxt.innerText = '인증번호 확인 완료.';
        }
    })
}