document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('diaryForm');
    if (!form) return;

    const maxImageCount = 10;
    const imageInput = document.getElementById('imageInput');
    const imageSelectButton = document.getElementById('imageSelectButton');
    const imagePreviewList = document.getElementById('imagePreviewList');
    const imageCount = document.getElementById('imageCount');
    const todoList = document.getElementById('todoList');
    const todoAddButton = document.getElementById('todoAddButton');
    const todoEmptyMessage = document.getElementById('todoEmptyMessage');
    const submitButton = form.querySelector('button[type="submit"]');
    let selectedFiles = [];

    function updateImageCount() {
        const count = imagePreviewList.querySelectorAll('.image-preview-item').length;
        imageCount.textContent = `${count} / ${maxImageCount}`;
        imageSelectButton.disabled = count >= maxImageCount;
    }

    function addImagePreview(file) {
        const previewUrl = URL.createObjectURL(file);
        const item = document.createElement('div');
        item.className = 'image-preview-item';

        const image = document.createElement('img');
        image.src = previewUrl;
        image.alt = '새 일기 이미지 미리보기';

        const removeButton = document.createElement('button');
        removeButton.type = 'button';
        removeButton.className = 'image-remove-button';
        removeButton.setAttribute('aria-label', '이미지 제거');
        removeButton.textContent = '×';
        removeButton.addEventListener('click', function() {
            selectedFiles = selectedFiles.filter(selectedFile => selectedFile !== file);
            URL.revokeObjectURL(previewUrl);
            item.remove();
            updateImageCount();
        });

        item.append(image, removeButton);
        imagePreviewList.append(item);
    }

    imagePreviewList.querySelectorAll('[data-existing-url] .image-remove-button').forEach(function(button) {
        button.addEventListener('click', function() {
            button.closest('.image-preview-item').remove();
            updateImageCount();
        });
    });

    imageSelectButton.addEventListener('click', function() {
        imageInput.click();
    });

    imageInput.addEventListener('change', function() {
        const currentCount = imagePreviewList.querySelectorAll('.image-preview-item').length;
        const availableCount = maxImageCount - currentCount;
        const files = Array.from(imageInput.files || []);

        if (files.length > availableCount) {
            showMessage(`이미지는 최대 ${maxImageCount}장까지 추가할 수 있습니다.`);
        }

        files.slice(0, availableCount).forEach(function(file) {
            selectedFiles.push(file);
            addImagePreview(file);
        });

        imageInput.value = '';
        updateImageCount();
    });

    function updateTodoEmptyMessage() {
        todoEmptyMessage.classList.toggle('is-hidden', todoList.querySelector('.todo-item') !== null);
    }

    function bindTodoRemoveButton(button) {
        button.addEventListener('click', function() {
            button.closest('.todo-item').remove();
            updateTodoEmptyMessage();
        });
    }

    todoList.querySelectorAll('.todo-remove-button').forEach(bindTodoRemoveButton);

    todoAddButton.addEventListener('click', function() {
        const item = document.createElement('div');
        item.className = 'todo-item';

        const completed = document.createElement('input');
        completed.className = 'todo-completed';
        completed.type = 'checkbox';
        completed.setAttribute('aria-label', '완료 여부');

        const content = document.createElement('input');
        content.className = 'form-control todo-content';
        content.type = 'text';
        content.maxLength = 255;
        content.placeholder = '할 일을 입력하세요';

        const removeButton = document.createElement('button');
        removeButton.className = 'btn btn-outline-danger btn-sm todo-remove-button';
        removeButton.type = 'button';
        removeButton.setAttribute('aria-label', '할 일 삭제');
        removeButton.textContent = '삭제';
        bindTodoRemoveButton(removeButton);

        item.append(completed, content, removeButton);
        todoList.append(item);
        content.focus();
        updateTodoEmptyMessage();
    });

    function showMessage(message) {
        const messageElement = document.getElementById('diaryMessage');
        messageElement.className = 'alert alert-danger';
        messageElement.textContent = message;
    }

    function uploadImages() {
        if (selectedFiles.length === 0) {
            return Promise.resolve([]);
        }

        const imageFormData = new FormData();
        selectedFiles.forEach(file => imageFormData.append('file', file));

        return fetch('/api/diary/image/upload', {
            method: 'POST',
            credentials: 'include',
            body: imageFormData
        })
        .then(async function(response) {
            const body = await response.json();
            if (!response.ok) {
                throw new Error(body.message || '이미지를 업로드하지 못했습니다');
            }
            return body.imageUrls || [];
        });
    }

    function getTodoPayload() {
        return Array.from(todoList.querySelectorAll('.todo-item'))
                .map(function(item, index) {
                    return {
                        content: item.querySelector('.todo-content').value.trim(),
                        completed: item.querySelector('.todo-completed').checked,
                        todoOrder: index
                    };
                })
                .filter(todo => todo.content.length > 0);
    }

    form.addEventListener('submit', async function(event) {
        event.preventDefault();

        const diaryId = form.dataset.diaryId;
        submitButton.disabled = true;

        try {
            const uploadedImageUrls = await uploadImages();
            const existingImageUrls = Array.from(imagePreviewList.querySelectorAll('[data-existing-url]'))
                    .map(item => item.dataset.existingUrl);
            const payload = {
                title: document.getElementById('title').value.trim(),
                content: document.getElementById('content').value,
                images: existingImageUrls.concat(uploadedImageUrls)
                        .map(imageUrl => ({ imageUrl })),
                todos: getTodoPayload()
            };

            if (diaryId) payload.id = Number(diaryId);

            const response = await fetch(diaryId ? '/api/diary/update' : '/api/diary/write', {
                method: diaryId ? 'PATCH' : 'POST',
                headers: {
                    'Content-Type': 'application/json; charset=utf-8'
                },
                credentials: 'include',
                body: JSON.stringify(payload)
            });
            const body = await response.json();
            if (!response.ok) {
                throw new Error(body.message || '일기를 저장하지 못했습니다');
            }
            window.location.href = '/diary/list';
        } catch (error) {
            showMessage(error.message);
            submitButton.disabled = false;
        }
    });

    updateImageCount();
    updateTodoEmptyMessage();
});
