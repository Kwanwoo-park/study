(function () {
    function bind(form) {
        if (!form?.dataset.draftKey) return;
        const key = 'study:text-draft:' + form.dataset.draftKey;
        const fields = [...form.querySelectorAll('[data-draft-field]')];
        const initial = Object.fromEntries(fields.map(field => [field.id, field.value]));
        const optIn = form.querySelector('[data-draft-opt-in]');
        const clearButton = form.querySelector('[data-draft-clear]');
        const status = form.querySelector('[data-draft-status]');
        let timer;

        function clear() {
            window.clearTimeout(timer);
            timer = undefined;
            try {
                localStorage.removeItem(key);
                clearButton.hidden = true;
                status.textContent = '';
            } catch (error) {
                status.textContent = '이 브라우저에서 임시저장에 접근할 수 없습니다.';
            }
        }

        function save() {
            timer = undefined;
            if (optIn && !optIn.checked) return;
            const values = Object.fromEntries(fields.map(field => [field.id, field.value]));
            if (fields.every(field => values[field.id] === initial[field.id])) {
                clear();
                return;
            }
            try {
                localStorage.setItem(key, JSON.stringify(values));
                clearButton.hidden = false;
                status.textContent = '이 브라우저에 임시저장했어요. 사진은 저장되지 않습니다.';
            } catch (error) {
                status.textContent = '이 브라우저에 임시저장하지 못했어요.';
            }
        }

        function scheduleSave() {
            if (optIn && !optIn.checked) return;
            window.clearTimeout(timer);
            timer = window.setTimeout(save, 250);
        }

        try {
            const saved = JSON.parse(localStorage.getItem(key));
            if (saved) {
                fields.forEach(field => {
                    if (typeof saved[field.id] === 'string') field.value = saved[field.id];
                });
                if (optIn) optIn.checked = true;
                clearButton.hidden = false;
                status.textContent = '임시저장한 글을 불러왔어요. 사진은 다시 선택해 주세요.';
            }
        } catch (error) {
            status.textContent = '임시저장한 글을 불러오지 못했어요.';
        }

        fields.forEach(field => field.addEventListener('input', scheduleSave));
        optIn?.addEventListener('change', () => {
            if (optIn.checked) save();
            else clear();
        });
        clearButton.addEventListener('click', clear);
        window.addEventListener('pagehide', () => {
            if (timer === undefined) return;
            window.clearTimeout(timer);
            save();
        });
        return {clear};
    }

    window.TextDraft = {bind};
})();
