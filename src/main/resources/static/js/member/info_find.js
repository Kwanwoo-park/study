const button = document.getElementById('find');
const birth = document.getElementById('birth');
const phone = document.getElementById('phone');

button.addEventListener('click', (event) => {
    event.preventDefault();

    if (birth.value && phone.value.trim()) {
        fetch(`/api/member/find/info?birth=` + birth.value + `&phone=` + phone.value, {
            credentials: "include",
        })
        .then((response) => response.json())
        .then((json) => {
            if (json['result'] > 0)
                window.location.href = '/member/findByEmail?email=' + encodeURIComponent(json.member.email);
            else
                alert("존재하지 않는 회원정보입니다.");
        })
        .catch((error) => {
            console.error(error)
            alert("존재하지 않는 회원정보입니다.");
        })
    }
})
