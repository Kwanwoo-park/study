const upload = document.getElementById('upload');
const btn = document.getElementById('btn');
const imgBtn = document.getElementById('imgBtn');
const submit = document.getElementById('submit');
const url = document.getElementById('url');
const description = document.getElementById('description');
const imgDiv = document.getElementById('imgDiv');
const container = document.querySelector('.container');
const imgGrid = document.querySelector('.imgGrid');
const selectToggle = document.getElementById('selectToggle');
const deleteBtn = document.getElementById('deleteBtn');
const selectedCount = document.getElementById('selectedCount');
const collectionCount = document.getElementById('collection-count');

const COLLECTION_LIMIT = 30;

let imgSrc = null;
let file;
let nextCursor = 1;
let isLoading = false;
let isSelectionMode = false;
const selectedCollectionIds = new Set();

window.onload = async function() {
    updateSelectionSummary();
    await loadMoreCollections();
};

if (btn) {
    btn.addEventListener('click', () => upload.click());
}

if (selectToggle) {
    selectToggle.addEventListener('click', toggleSelectionMode);
}

if (container) {
    container.addEventListener('scroll', () => {
        if (container.scrollTop + container.clientHeight >= container.scrollHeight - 1) {
            loadMoreCollections();
        }
    });
}

async function loadMoreCollections() {
    if (!imgGrid || !nextCursor || isLoading) {
        return;
    }

    isLoading = true;

    try {
        const response = await fetch(`/api/collection/load?cursor=${nextCursor - 1}&limit=${COLLECTION_LIMIT}`, {
            method: 'GET',
            credentials: 'include',
        });

        const json = await response.json();

        if (json.result > 0) {
            drawCollections(json.collection);
            nextCursor = json.nextCursor;

            if (!nextCursor) {
                const load = document.createElement('span')
                load.id = 'loading'
                load.innerText = '모든 게시물을 불러왔습니다';
                imgGrid.append(load);
            }
        } else {
            alert('다시 시도하여주십시오');
        }
    } catch (error) {
        console.error(error);
        alert('다시 시도하여주십시오');
    } finally {
        isLoading = false;
    }
}

function drawCollections(collections) {
    collections.forEach((item) => {
        const wrapper = document.createElement('div');
        wrapper.className = 'mainImgDiv';
        wrapper.dataset.id = String(item.id);
        wrapper.dataset.url = item.url ?? '';

        const image = document.createElement('img');
        image.className = 'main-image';
        image.src = item.imgSrc;
        image.alt = item.description ?? '';
        image.loading = 'lazy';

        const desc = document.createElement('span');
        desc.className = 'main-desc';
        desc.innerText = item.description ?? '';

        const status = document.createElement('small');
        status.className = 'selection-status d-none';
        status.innerText = '선택됨';

        wrapper.addEventListener('click', function() {
            if (isSelectionMode) {
                toggleCollectionSelection(wrapper.dataset.id);
                return;
            }

            if (item.url) {
                window.open(item.url, '_blank');
            }
        });

        wrapper.append(image);
        wrapper.append(desc);
        wrapper.append(status);
        syncCardSelectionState(wrapper);

        imgGrid.append(wrapper);
    });
}

async function fnSave() {
    await fnImgSave();

    if (!url || !description || !imgSrc) return;

    const data = {
        url: url.value,
        description: description.value,
        imgSrc: imgSrc
    };

    try {
        const response = await fetch(`/api/collection/save/collection`, {
            method: 'POST',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            body: JSON.stringify(data),
            credentials: "include",
        });

        const json = await response.json();

        if (json.result > 0)
            window.location.reload();
        else
            alert("다시 시도하여주십시오");
    } catch (error) {
        console.error(error);
        alert("다시 시도하여주십시오");
    }
}

async function fnImgSave() {
    const formData = new FormData();
    formData.append("file", file);

    try {
        const response = await fetch(`/api/collection/save/img`, {
            method: 'POST',
            body: formData,
            credentials: "include",
        });

        const json = await response.json();

        if (json.result < 0) {
            alert('사진 등록에 실패하였습니다');
        } else {
            imgSrc = json.imgSrc;
        }
    } catch (error) {
        console.error(error);
        alert("다시 시도하여주십시오");
    }
}

async function fnDelete() {
    if (!selectedCollectionIds.size) {
        alert('삭제할 게시글을 선택하여 주십시오');
        return;
    }

    if (!confirm('선택한 게시글을 삭제하시겠습니까?')) {
        return;
    }

    const ids = Array.from(selectedCollectionIds);
    let successCount = 0;
    let failCount = 0;

    if (deleteBtn) {
        deleteBtn.disabled = true;
    }

    try {
        const response = await fetch(`/api/collection/delete`, {
            method: 'DELETE',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            body: JSON.stringify(ids),
            credentials: 'include',
        });

        const json = await response.json();

        if (response.ok && json.result > 0) {
            successCount += ids.length;
        }
    } catch (error) {
        console.error(error);
    }

    if (successCount > 0) {
        updateCollectionCount(-successCount);
    }

    document.querySelectorAll('.mainImgDiv').forEach((card) => {
        syncCardSelectionState(card);
    });

    selectedCollectionIds.clear();
    updateSelectionSummary();

    if (successCount > 0 && failCount === 0) {
        removeCollectionCard(ids)
        alert('선택한 게시글이 삭제되었습니다');
        return;
    }

    if (successCount > 0) {
        alert(`${successCount}개 삭제, ${failCount}개 실패하였습니다`);
        return;
    }

    alert('게시글 삭제 중 오류가 발생하였습니다');
}

function fnLoad(input) {
    file = input.files[0];

    const preview = document.createElement('img');
    preview.className = 'save-img';
    preview.src = URL.createObjectURL(file);

    btn.style.display = 'none';
    submit.style.display = 'inline';

    const oldPreview = imgDiv.querySelector('.save-img');
    if (oldPreview) {
        oldPreview.remove();
    }

    imgDiv.append(preview);
}

function toggleSelectionMode() {
    isSelectionMode = !isSelectionMode;

    if (!isSelectionMode) {
        selectedCollectionIds.clear();
    }

    document.querySelectorAll('.mainImgDiv').forEach((card) => {
        syncCardSelectionState(card);
    });

    updateSelectionSummary();
}

function toggleCollectionSelection(id) {
    if (selectedCollectionIds.has(id)) {
        selectedCollectionIds.delete(id);
    } else {
        selectedCollectionIds.add(id);
    }

    const card = document.querySelector(`.mainImgDiv[data-id="${id}"]`);
    if (card) {
        syncCardSelectionState(card);
    }

    updateSelectionSummary();
}

function syncCardSelectionState(card) {
    const isSelected = selectedCollectionIds.has(card.dataset.id);
    const status = card.querySelector('.selection-status');

    card.classList.toggle('border-primary', isSelected);
    card.classList.toggle('bg-light', isSelected);
    card.classList.toggle('shadow-sm', isSelected);
    card.style.cursor = isSelectionMode || card.dataset.url ? 'pointer' : 'default';
    card.setAttribute('aria-pressed', String(isSelected));

    if (!status) {
        return;
    }

    if (!isSelectionMode) {
        status.classList.add('d-none');
        status.classList.remove('text-primary');
        status.classList.remove('text-muted');
        return;
    }

    status.classList.remove('d-none');
    status.innerText = isSelected ? '선택됨' : '선택하려면 클릭';
    status.classList.toggle('text-primary', isSelected);
    status.classList.toggle('text-muted', !isSelected);
}

function updateSelectionSummary() {
    if (selectedCount) {
        selectedCount.innerText = `선택된 게시글 ${selectedCollectionIds.size}개`;
    }

    if (selectToggle) {
        selectToggle.innerText = isSelectionMode ? '선택 모드 종료' : '선택 모드';
        selectToggle.classList.toggle('btn-secondary', isSelectionMode);
        selectToggle.classList.toggle('btn-outline-secondary', !isSelectionMode);
    }

    if (deleteBtn) {
        deleteBtn.disabled = !isSelectionMode || selectedCollectionIds.size === 0;
    }
}

function removeCollectionCard(ids) {
    for (const id of ids) {
        const card = document.querySelector(`.mainImgDiv[data-id="${id}"]`);

        if (card) {
            card.remove();
        }
    }
}

function updateCollectionCount(diff) {
    if (!collectionCount) {
        return;
    }

    const currentCount = Number(collectionCount.innerText);
    collectionCount.innerText = Math.max(0, currentCount + diff);
}
