'use strict';

const planEditor = document.querySelector('.plan-editor');
if (planEditor) {
    const feedback = planEditor.querySelector('[data-edit-feedback]');
    const save = planEditor.querySelector('button[value="save"]');
    const actionsBar = planEditor.querySelector('.editor-actions');

    function invalidatePreview() {
        if (save) {
            save.disabled = true;
            save.setAttribute('aria-disabled', 'true');
        }
        if (actionsBar) {
            actionsBar.classList.add('is-pending-preview');
        }
        if (feedback) {
            feedback.textContent = 'Changes pending. Preview to review the impact.';
        }
    }

    for (const day of planEditor.querySelectorAll('.editor-day')) {
        const rows = day.querySelector('[data-exercise-rows]');
        const add = day.querySelector('[data-add-exercise]');
        const template = day.querySelector('template');
        const countBadge = day.querySelector('[data-day-count]');

        function refreshRows() {
            const exercises = [...rows.querySelectorAll('[data-exercise-row]')];
            exercises.forEach((row, index) => {
                const legend = row.querySelector('legend');
                if (legend) legend.textContent = 'Exercise ' + (index + 1);
                const removeBtn = row.querySelector('[data-remove-exercise]');
                if (removeBtn) removeBtn.hidden = false;
            });
            if (countBadge) {
                countBadge.textContent = exercises.length + ' / 8 exercises';
            }
            if (add) {
                add.disabled = exercises.length >= 8;
                add.classList.toggle('is-disabled', exercises.length >= 8);
            }
        }

        if (add && template && rows) {
            add.hidden = false;
            refreshRows();

            add.addEventListener('click', () => {
                const fragment = template.content.cloneNode(true);
                rows.append(fragment);
                const addedElement = rows.lastElementChild;
                if (addedElement) {
                    addedElement.classList.add('row-anim-enter');
                }
                refreshRows();
                rows.lastElementChild?.querySelector('select')?.focus();
                invalidatePreview();
            });

            rows.addEventListener('click', event => {
                const remove = event.target.closest('[data-remove-exercise]');
                if (!remove) return;
                const row = remove.closest('[data-exercise-row]');
                if (!row) return;

                const next = row.nextElementSibling || row.previousElementSibling;
                row.classList.add('row-anim-leave');
                setTimeout(() => {
                    row.remove();
                    refreshRows();
                    (next?.querySelector('select') || add).focus();
                    invalidatePreview();
                }, 160);
            });
        }
    }

    for (const pill of planEditor.querySelectorAll('[data-jump-day]')) {
        pill.addEventListener('click', () => {
            const weekday = pill.getAttribute('data-jump-day');
            const targetSelect = planEditor.querySelector('select[name="type-' + weekday + '"]');
            const targetDay = targetSelect?.closest('.editor-day');
            if (targetDay) {
                targetDay.open = true;
                targetDay.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                targetSelect.focus();
            }
        });
    }

    planEditor.addEventListener('input', invalidatePreview);
    planEditor.addEventListener('invalid', event => {
        const day = event.target.closest('.editor-day');
        if (day) day.open = true;
    }, true);
}
