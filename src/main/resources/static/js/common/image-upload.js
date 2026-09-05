(function(global) {
    'use strict';

    const readErrorMessage = '선택한 사진을 읽을 수 없습니다. 사진을 다시 선택하여 주십시오.';

    function resolveFileName(file, index) {
        const originalName = typeof file.name === 'string' ? file.name.trim() : '';
        if (originalName) return originalName;

        const extensionByType = {
            'image/jpeg': 'jpg',
            'image/png': 'png',
            'image/gif': 'gif',
        };
        const extension = extensionByType[file.type] || 'bin';
        return `mobile-image-${Date.now()}-${index}.${extension}`;
    }

    async function snapshotFile(file, index) {
        try {
            // Retain the bytes, not the photo picker's temporary file reference.
            const bytes = await file.arrayBuffer();
            if (bytes.byteLength === 0) throw new Error(readErrorMessage);

            return new File([bytes], resolveFileName(file, index), {
                type: file.type || 'application/octet-stream',
                lastModified: file.lastModified || Date.now(),
            });
        } catch (error) {
            throw new Error(readErrorMessage);
        }
    }

    async function snapshotFiles(files) {
        return Promise.all(Array.from(files || []).map(snapshotFile));
    }

    function buildFormData(files) {
        const images = Array.from(files || []);
        if (images.length === 0 || images.some(file => !(file instanceof Blob) || file.size === 0)) {
            throw new Error('업로드할 이미지를 다시 선택하여 주십시오.');
        }

        const formData = new FormData();
        images.forEach((file, index) => {
            formData.append('file', file, resolveFileName(file, index));
        });
        return formData;
    }

    global.ImageUpload = Object.freeze({ snapshotFiles, buildFormData });
})(window);
