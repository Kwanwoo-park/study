const button = document.getElementById('submit')

if (button) {
    button.addEventListener('click', (event) => {
        event.preventDefault();

        fetch(`/api/member/withdrawal`, {
            method: 'DELETE',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
        })
        .then((response) => {
            if (response.status == 200) {
                alert("삭제되었습니다");
                location.replace(`/member/login`)
            }
           else {
                alert("다시 시도하여주십시오");
           }
        })
        .catch((error) => {
            console.error(error)
            alert("다시 시도하여주십시오");
        })
    })
}