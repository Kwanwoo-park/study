document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('diaryForm');
    if (!form) return;

    const maxImageCount = 10;
    const imageInput = document.getElementById('imageInput');
    const imageSelectButton = document.getElementById('imageSelectButton');
    const imagePreviewList = document.getElementById('imagePreviewList');
    const imageCount = document.getElementById('imageCount');
    const submitButton = form.querySelector('button[type="submit"]');
    const deleteButton = document.getElementById('diaryDeleteButton');
    let selectedFiles = [];
    let isPreparingImages = false;
    let isSaving = false;

    function updateImageCount() {
        const count = imagePreviewList.querySelectorAll('.image-preview-item').length;
        imageCount.textContent = `${count} / ${maxImageCount}`;
        const busy = isPreparingImages || isSaving;
        imageSelectButton.disabled = busy || count >= maxImageCount;
        imageInput.disabled = busy;
        submitButton.disabled = busy;
        if (deleteButton) deleteButton.disabled = busy;
        imagePreviewList.querySelectorAll('.image-remove-button').forEach(button => {
            button.disabled = busy;
        });
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
            if (isPreparingImages || isSaving) return;
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
            if (isPreparingImages || isSaving) return;
            button.closest('.image-preview-item').remove();
            updateImageCount();
        });
    });

    imageSelectButton.addEventListener('click', function() {
        if (isPreparingImages || isSaving) return;
        imageInput.click();
    });

    imageInput.addEventListener('change', async function() {
        if (isPreparingImages || isSaving) return;
        const currentCount = imagePreviewList.querySelectorAll('.image-preview-item').length;
        const availableCount = maxImageCount - currentCount;
        const files = Array.from(imageInput.files || []);
        if (files.length === 0) return;

        if (files.length > availableCount) {
            showMessage(`이미지는 최대 ${maxImageCount}장까지 추가할 수 있습니다.`);
        }

        isPreparingImages = true;
        updateImageCount();
        try {
            const snapshots = await ImageUpload.snapshotFiles(files.slice(0, availableCount));
            snapshots.forEach(function(file) {
                selectedFiles.push(file);
                addImagePreview(file);
            });
            imageInput.value = '';
        } catch (error) {
            imageInput.value = '';
            showMessage(error.message);
        } finally {
            isPreparingImages = false;
            updateImageCount();
        }
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

        const imageFormData = ImageUpload.buildFormData(selectedFiles);

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

    form.addEventListener('submit', async function(event) {
        event.preventDefault();
        if (isPreparingImages || isSaving) return;

        const diaryId = form.dataset.diaryId;
        isSaving = true;
        updateImageCount();

        try {
            const uploadedImageUrls = await uploadImages();
            const existingImageUrls = Array.from(imagePreviewList.querySelectorAll('[data-existing-url]'))
                    .map(item => item.dataset.existingUrl);
            const payload = {
                title: document.getElementById('title').value.trim(),
                content: document.getElementById('content').value,
                visibility: document.getElementById('visibility').value,
                images: existingImageUrls.concat(uploadedImageUrls)
                        .map(imageUrl => ({ imageUrl }))
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
            window.location.replace('/diary/list');
        } catch (error) {
            showMessage(error.message);
        } finally {
            isSaving = false;
            updateImageCount();
        }
    });

    if (deleteButton) {
        deleteButton.addEventListener('click', async function() {
            if (isPreparingImages || isSaving) return;
            const diaryId = form.dataset.diaryId;
            if (!diaryId || !confirm('이 일기를 삭제하시겠습니까?')) return;

            isSaving = true;
            updateImageCount();

            try {
                const response = await fetch(`/api/diary/${encodeURIComponent(diaryId)}`, {
                    method: 'DELETE',
                    credentials: 'include'
                });
                const body = await response.json();
                if (!response.ok) {
                    throw new Error(body.message || '일기를 삭제하지 못했습니다');
                }
                window.location.replace('/diary/list');
            } catch (error) {
                showMessage(error.message);
            } finally {
                isSaving = false;
                updateImageCount();
            }
        });
    }

    updateImageCount();
});
