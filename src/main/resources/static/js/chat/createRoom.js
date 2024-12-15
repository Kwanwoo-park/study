const button = document.getElementById('create');
const name = document.getElementById('name');

if (button) {
    button.addEventListener('click', (event) => {
        event.preventDefault();
        if (name.value != '') {
            fetch(`/api/chat/createRoom?name=` + name.value, {
                method: 'POST',
                headers: {
                    "Content-Type": "application/json; charset=utf-8",
                },
                body: JSON.stringify(name.value),
            })
            .then((response) => response.json())
            .then((json) => {
                location.href = '/chat/chatRoom?roomId=' + json.roomId;
            })
            .catch((error) => {
                console.error(error)
                alert("다시 시도하여주십시오");
            });
        }
    });
}

if (name) {
    name.addEventListener('keydown', (event) => {
        if (event.key == 'Enter')
            button.click();
    });
}