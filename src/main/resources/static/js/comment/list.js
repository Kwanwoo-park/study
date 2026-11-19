const commentPanel = createCommentPanel({
    list: document.getElementById('commentList'),
    input: document.getElementById('comments'),
    submit: document.getElementById('submit'),
    cancel: document.getElementById('cancel'),
    standalone: true,
});

commentPanel.open(document.querySelector('.comment-list-page main').dataset.boardId);
