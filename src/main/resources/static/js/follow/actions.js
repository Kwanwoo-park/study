async function toggleMemberFollow(button) {
    if (button.disabled) return;
    const following = button.textContent.trim() === 'Unfollow';
    button.disabled = true;
    try {
        const response = await fetch('/api/follow', {
            method: following ? 'DELETE' : 'POST',
            headers: {'Content-Type': 'application/json; charset=utf-8'},
            body: JSON.stringify({email: button.dataset.email}),
            credentials: 'include',
        });
        const data = await response.json();
        if (!response.ok || data.result <= 0) throw new Error('팔로우 변경 실패');
        button.textContent = following ? 'Follow' : 'Unfollow';
        return true;
    } catch (error) {
        console.error(error);
        alert('다시 시도하여주십시오');
        return false;
    } finally {
        button.disabled = false;
    }
}
