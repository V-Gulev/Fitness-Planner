'use strict';

const profileForm = document.querySelector('.profile-workspace .profile-form');
if (profileForm) {
    const minutes = profileForm.querySelector('#sessionMinutes');
    const presets = profileForm.querySelector('[data-duration-presets]');
    const summary = profileForm.querySelector('[data-profile-summary]');
    const exercises = [...profileForm.querySelectorAll('[data-exercise]')];
    const search = profileForm.querySelector('[data-exercise-search]');

    function updateSummary() {
        const goal = profileForm.querySelector('[name="goal"]:checked');
        const days = profileForm.querySelectorAll('[name="weekdays"]:checked').length;
        const equipment = profileForm.querySelectorAll('[name="equipment"]:checked').length;
        summary.querySelector('[data-summary-goal]').textContent =
            goal?.closest('label').querySelector('span').textContent || 'Choose a goal';
        summary.querySelector('[data-summary-days]').textContent =
            days + (days === 1 ? ' day per week' : ' days per week');
        summary.querySelector('[data-summary-time]').textContent =
            minutes.validity.valid ? minutes.value + ' min' : 'Enter 20–120 min';
        summary.querySelector('[data-summary-equipment]').textContent =
            equipment === 0 ? 'Bodyweight' : equipment + (equipment === 1 ? ' item' : ' items');
        for (const button of presets.querySelectorAll('button')) {
            button.setAttribute('aria-pressed', String(button.dataset.minutes === minutes.value));
        }
    }

    presets.hidden = false;
    summary.hidden = false;
    presets.addEventListener('click', event => {
        const button = event.target.closest('[data-minutes]');
        if (!button) return;
        minutes.value = button.dataset.minutes;
        updateSummary();
    });
    profileForm.addEventListener('input', updateSummary);
    profileForm.addEventListener('change', event => {
        const input = event.target;
        const row = input.closest('[data-exercise]');
        if (row && input.checked) {
            for (const choice of row.querySelectorAll('input[type="checkbox"]')) {
                if (choice !== input) choice.checked = false;
            }
        }
    });
    profileForm.addEventListener('invalid', event => {
        const disclosure = event.target.closest('details');
        if (disclosure) disclosure.open = true;
    }, true);

    search.closest('label').hidden = false;
    search.addEventListener('input', () => {
        const term = search.value.trim().toLowerCase();
        for (const row of exercises) {
            row.hidden = !row.firstElementChild.textContent.toLowerCase().includes(term);
        }
        profileForm.querySelector('[data-no-exercises]').hidden = exercises.some(row => !row.hidden);
    });
    updateSummary();
}
