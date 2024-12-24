const title = document.getElementById('title');

if (title) {
    title.addEventListener('keydown', (event) => {
        if (event.key == 'Enter')
            location.replace(`/board/list?title=` + title.value);
    })
}