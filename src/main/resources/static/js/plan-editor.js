'use strict';

const planEditor = document.querySelector('.plan-editor');
if (planEditor) {
    const feedback = planEditor.querySelector('[data-edit-feedback]');
    const save = planEditor.querySelector('button[value="save"]');
    const actionsBar = planEditor.querySelector('.editor-actions');
    let addedRowCount = 0;

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

    function updateTotalExercises() {
        const totalBadge = planEditor.querySelector('[data-total-exercises]');
        if (!totalBadge) return;
        const count = planEditor.querySelectorAll('[data-exercise-row]').length;
        totalBadge.textContent = count === 1 ? '1 movement planned' : count + ' movements planned';
    }

    function initStepper(container) {
        container.querySelectorAll('.stepper-input').forEach(stepper => {
            if (stepper.dataset.stepperBound) return;
            stepper.dataset.stepperBound = 'true';

            const input = stepper.querySelector('input');
            const downBtn = stepper.querySelector('.stepper-down');
            const upBtn = stepper.querySelector('.stepper-up');
            if (!input) return;

            const step = parseFloat(input.dataset.stepperStep || input.step) || 1;
            const min = input.min !== '' ? parseFloat(input.min) : -Infinity;
            const max = input.max !== '' ? parseFloat(input.max) : Infinity;

            function adjust(delta) {
                let val = parseFloat(input.value);
                if (isNaN(val)) val = min !== -Infinity ? min : 0;
                let next = val + delta;
                if (next < min) next = min;
                if (next > max) next = max;
                input.value = next;
                input.dispatchEvent(new Event('input', { bubbles: true }));
                input.dispatchEvent(new Event('change', { bubbles: true }));
            }

            downBtn?.addEventListener('click', (e) => {
                e.preventDefault();
                adjust(-step);
            });

            upBtn?.addEventListener('click', (e) => {
                e.preventDefault();
                adjust(step);
            });
        });
    }

    for (const day of planEditor.querySelectorAll('.editor-day')) {
        const rows = day.querySelector('[data-exercise-rows]');
        const add = day.querySelector('[data-add-exercise]');
        const template = day.querySelector('template');
        const countBadge = day.querySelector('[data-day-count]');

        function refreshRows() {
            const exercises = [...rows.querySelectorAll('[data-exercise-row]')];
            exercises.forEach((row, index) => {
                const legend = row.querySelector('.editor-row-legend') || row.querySelector('legend');
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
            updateTotalExercises();
        }

        if (add && template && rows) {
            add.hidden = false;
            refreshRows();

            add.addEventListener('click', () => {
                const fragment = template.content.cloneNode(true);
                const suffix = '-added-' + ++addedRowCount;
                fragment.querySelectorAll('input[id]').forEach(input => { input.id += suffix; });
                fragment.querySelectorAll('label[for]').forEach(label => { label.htmlFor += suffix; });
                rows.append(fragment);
                const addedElement = rows.lastElementChild;
                if (addedElement) {
                    addedElement.classList.add('row-anim-enter');
                    initStepper(addedElement);
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
                row.disabled = true;
                invalidatePreview();
                row.classList.add('row-anim-leave');
                setTimeout(() => {
                    row.remove();
                    refreshRows();
                    (next?.querySelector('select') || add).focus();
                }, 160);
            });
        }
    }

    // Day toolbar filter tabs
    const filterButtons = planEditor.querySelectorAll('.editor-days-toolbar [data-day-filter]');
    const dayCards = planEditor.querySelectorAll('.editor-day');

    filterButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const filter = btn.getAttribute('data-day-filter');
            filterButtons.forEach(b => {
                const isActive = b === btn;
                b.classList.toggle('is-active', isActive);
                b.setAttribute('aria-selected', isActive ? 'true' : 'false');
            });

            if (filter === 'all') {
                dayCards.forEach(card => card.classList.remove('is-card-hidden'));
            } else {
                dayCards.forEach(card => {
                    const match = card.getAttribute('data-weekday') === filter;
                    card.classList.toggle('is-card-hidden', !match);
                    if (match) {
                        card.open = true;
                        card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                    }
                });
            }
        });
    });

    // Dynamic split type selector synchronization
    planEditor.querySelectorAll('.day-focus-select').forEach(select => {
        select.addEventListener('change', () => {
            const weekday = select.name.replace('type-', '');
            const selectedText = select.options[select.selectedIndex]?.text || '';
            const tabPill = document.getElementById('tab-type-' + weekday);
            if (tabPill) tabPill.textContent = selectedText;
            const headerType = document.getElementById('header-type-' + weekday);
            if (headerType) headerType.textContent = selectedText;
            invalidatePreview();
        });
    });

    // Jump navigation fallback
    for (const pill of planEditor.querySelectorAll('[data-jump-day]')) {
        pill.addEventListener('click', () => {
            const weekday = pill.getAttribute('data-jump-day');
            const targetSelect = planEditor.querySelector('select[name="type-' + weekday + '"]');
            const targetDay = targetSelect?.closest('.editor-day');
            if (targetDay) {
                targetDay.classList.remove('is-card-hidden');
                targetDay.open = true;
                targetDay.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                targetSelect.focus();
            }
        });
    }

    // Steppers and initial counts
    initStepper(planEditor);
    updateTotalExercises();

    planEditor.addEventListener('input', invalidatePreview);
    planEditor.addEventListener('invalid', event => {
        const day = event.target.closest('.editor-day');
        if (day) {
            day.classList.remove('is-card-hidden');
            day.open = true;
        }
    }, true);
}
