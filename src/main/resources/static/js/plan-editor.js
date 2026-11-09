'use strict';

const planEditor = document.querySelector('.plan-editor');
if (planEditor) {
    const feedback = planEditor.querySelector('[data-edit-feedback]');
    const save = planEditor.querySelector('button[value="save"]');

    function invalidatePreview() {
        if (save) save.disabled = true;
        feedback.textContent = 'Changes pending. Preview to review the impact.';
    }

    for (const day of planEditor.querySelectorAll('.editor-day')) {
        const rows = day.querySelector('[data-exercise-rows]');
        const add = day.querySelector('[data-add-exercise]');
        const template = day.querySelector('template');

        function refreshRows() {
            const exercises = [...rows.querySelectorAll('[data-exercise-row]')];
            exercises.forEach((row, index) => {
                row.querySelector('legend').textContent = 'Exercise ' + (index + 1);
                row.querySelector('[data-remove-exercise]').hidden = false;
            });
            add.disabled = exercises.length >= 8;
        }

        add.hidden = false;
        refreshRows();
        add.addEventListener('click', () => {
            rows.append(template.content.cloneNode(true));
            refreshRows();
            rows.lastElementChild.querySelector('select').focus();
            invalidatePreview();
        });
        rows.addEventListener('click', event => {
            const remove = event.target.closest('[data-remove-exercise]');
            if (!remove) return;
            const row = remove.closest('[data-exercise-row]');
            const next = row.nextElementSibling || row.previousElementSibling;
            row.remove();
            refreshRows();
            (next?.querySelector('select') || add).focus();
            invalidatePreview();
        });
    }
    planEditor.addEventListener('input', invalidatePreview);
    planEditor.addEventListener('invalid', event => {
        const day = event.target.closest('.editor-day');
        if (day) day.open = true;
    }, true);
}
