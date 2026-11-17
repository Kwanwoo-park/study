const upload = document.querySelector("#upload");
const profile = document.querySelector("#profile");

let file;
let profilePreviewUrl = null;
let isPreparingProfileImage = false;
let isSavingProfileImage = false;
const profileSaveButton = document.getElementById('save');
profile.addEventListener('click', () => upload.click());

async function fnLoad(input) {
    if (isPreparingProfileImage || isSavingProfileImage) return;
    const selectedFile = input.files[0];
    if (!selectedFile) return;

    isPreparingProfileImage = true;
    input.disabled = profileSaveButton.disabled = true;
    try {
        const [snapshot] = await ImageUpload.snapshotFiles([selectedFile]);
        const nextPreviewUrl = URL.createObjectURL(snapshot);
        if (profilePreviewUrl) URL.revokeObjectURL(profilePreviewUrl);
        file = snapshot;
        profilePreviewUrl = nextPreviewUrl;
        profile.src = profilePreviewUrl;
        input.value = '';
        profileSaveButton.classList.remove('is-hidden');
    } catch (error) {
        input.value = '';
        alert(error.message);
    } finally {
        isPreparingProfileImage = false;
        input.disabled = profileSaveButton.disabled = false;
    }
}

async function fnSave() {
    if (isPreparingProfileImage || isSavingProfileImage) return;
    if (!file) {
        alert('업로드할 이미지를 다시 선택하여 주십시오.');
        return;
    }

    isSavingProfileImage = true;
    upload.disabled = profileSaveButton.disabled = true;
    try {
        const response = await fetch(`/api/member/detail/action`, {
            method: 'PATCH',
            body: ImageUpload.buildFormData([file]),
            credentials: "include",
        });
        const json = await response.json();
        const result = json['result'];

        if (!response.ok || !(result > 0)) {
            alert(json.message || "사진 변경에 실패했습니다");
        } else {
            alert("사진이 변경되었습니다.");
            window.location.reload();
        }
    } catch (error) {
        alert("사진 변경에 실패했습니다");
    } finally {
        isSavingProfileImage = false;
        upload.disabled = profileSaveButton.disabled = false;
    }
}
