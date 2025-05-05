const word = document.getElementById("word");
const btn = document.getElementById("btn");
const div = document.getElementById("uploadDiv")
const risk = document.getElementById('risk')

if (btn) {
    btn.addEventListener('click', (event) => {
        if (div.style.display === 'none')
            div.style.display = 'inline';
        else {
            fetch(`/api/admin/forbidden/word/save`, {
                method: 'POST',
                headers: {
                    "Content-Type": "application/json; charset=utf-8",
                },
                body: JSON.stringify({
                    word: word.value,
                    risk: risk.value
                }),
            })
            .then((response) => response.json())
            .then((json) => {
                window.location.reload();
            })
            .catch((error) => {
                alert("다시 시도하여 주십시오");
            })
        }
    })
}