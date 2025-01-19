const title = document.getElementById('title');

if (title) {
    title.addEventListener('keydown', (event) => {
        if (event.key == 'Enter') {
            let temp = title.value;
            title.value = null;
            location.href = "/board/list?title=" + temp;
        }
    })
}