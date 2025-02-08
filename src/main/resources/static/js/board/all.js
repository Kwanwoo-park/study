const title = document.getElementById('title');
const clear = document.querySelector(".btnClear")

if (title) {
    title.addEventListener('keydown', (event) => {
        if (event.key == 'Enter') {
            //console.log(title.value)
            location.href = "/board/all?title=" + title.value;
        }
    })
}

if (clear) {
    clear.addEventListener('click', (event) => {
        title.value = null
    })
}