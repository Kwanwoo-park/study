(function() {
    try {
        if (localStorage.getItem('theme') === 'dark' && document.body) {
            document.body.classList.add('dark-mode');
        }
    } catch (error) {
        console.error('테마 조회 오류:', error);
    }
})();
