'use strict';
document.querySelectorAll('.add-set').forEach(button => {
    const fieldset = button.previousElementSibling;
    const rows = () => fieldset.querySelectorAll('.set-row');
    const update = () => { button.disabled = rows().length >= 12; };
    button.hidden = false;
    update();
    button.addEventListener('click', () => {
        const index = rows().length;
        if (index >= 12) return;
        const copy = rows()[0].cloneNode(true);
        copy.querySelector('.set-label').textContent = `Set ${index + 1}`;
        copy.querySelectorAll('input').forEach(input => {
            input.name = input.name.replace(/\.sets\[\d+\]/, `.sets[${index}]`);
            input.removeAttribute('id');
            input.setAttribute('aria-label', input.getAttribute('aria-label').replace(/set \d+/, `set ${index + 1}`));
            input.value = '';
        });
        fieldset.append(copy);
        update();
        copy.querySelector('input').focus();
    });
});
