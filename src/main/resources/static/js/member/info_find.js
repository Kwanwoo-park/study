const button = document.getElementById('find');
const birth = document.getElementById('birth');
const phone = document.getElementById('phone');

if (button) {
    button.addEventListener('click', (event) => {
        event.preventDefault();

        if (birth.value != '' && phone != '') {
            fetch(`/api/member/find/info?birth=` + birth.value + `&phone=` + phone.value, {
                method: 'GET',
                headers: {
                    "Content-Type": "application/json; charset=utf-8",
                },
                credentials: "include",
            })
            .then((response) => response.json())
            .then((json) => {
                if (json['result'] > 0)
                    location.href = "/member/updatePassword/" + json.member.email;
                else
                    alert("존재하지 않는 회원정보입니다.");
            })
            .catch((error) => {
                console.error(error)
                alert("존재하지 않는 회원정보입니다.");
            })
        }
    })
}