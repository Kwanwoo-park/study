const name = document.getElementById('name');

if (name) {
    name.addEventListener('keydown', (event) => {
        if (event.key == 'Enter')
            location.replace(`/member/search/` + name.value);
    })
}