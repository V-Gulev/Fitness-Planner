'use strict';

/**
 * Interactive behaviors for /login and /register pages:
 * - Password visibility toggle (accessible, SVG morphing)
 * - Real-time password strength and requirement validation (on register)
 * - Live password match confirmation (on register)
 * - Form submit feedback and duplicate submission prevention
 */
document.addEventListener('DOMContentLoaded', () => {
    initPasswordToggles();
    initRegisterInteractions();
    initFormSubmission();
    initAutofocus();
});

function initPasswordToggles() {
    const toggleButtons = document.querySelectorAll('[data-password-toggle]');
    toggleButtons.forEach(button => {
        button.addEventListener('click', () => {
            const wrapper = button.closest('.input-affix-wrapper');
            if (!wrapper) return;
            const input = wrapper.querySelector('input');
            if (!input) return;

            const isPassword = input.type === 'password';
            input.type = isPassword ? 'text' : 'password';

            button.setAttribute('aria-pressed', String(isPassword));
            button.setAttribute('aria-label', isPassword ? 'Hide password' : 'Show password');
            button.setAttribute('title', isPassword ? 'Hide password' : 'Show password');

            const eyeOpen = button.querySelector('.icon-eye-open');
            const eyeClosed = button.querySelector('.icon-eye-closed');
            if (eyeOpen && eyeClosed) {
                eyeOpen.style.display = isPassword ? 'none' : 'block';
                eyeClosed.style.display = isPassword ? 'block' : 'none';
            }

            input.focus();
        });
    });
}

function initRegisterInteractions() {
    const passwordInput = document.getElementById('password');
    const confirmInput = document.getElementById('confirmPassword');
    const strengthBar = document.querySelector('[data-strength-bar]');
    const strengthText = document.querySelector('[data-strength-text]');
    const matchStatus = document.querySelector('[data-match-status]');
    const reqLength = document.querySelector('[data-req="length"]');
    const reqVariety = document.querySelector('[data-req="variety"]');

    if (!passwordInput || !strengthBar) return;

    function evaluateStrength(value) {
        if (!value) return { score: 0, label: 'Enter password' };
        if (value.length < 10) return { score: 1, label: 'Too short (min 10 chars)' };

        let score = 1;
        const hasNumbers = /\d/.test(value);
        const hasLetters = /[a-zA-Z]/.test(value);
        const hasSpecial = /[^a-zA-Z0-9]/.test(value);
        const isLong = value.length >= 14;

        if (hasLetters && hasNumbers) score++;
        if (hasSpecial) score++;
        if (isLong && score >= 2) score++;

        score = Math.min(score, 4);

        const labels = {
            1: 'Too short',
            2: 'Fair',
            3: 'Good',
            4: 'Strong'
        };

        return { score, label: labels[score] || 'Fair' };
    }

    function update() {
        const val = passwordInput.value || '';
        const confirmVal = confirmInput ? confirmInput.value || '' : '';

        // Password strength meter
        const { score, label } = evaluateStrength(val);
        strengthBar.setAttribute('data-score', String(score));
        if (strengthText) {
            strengthText.textContent = label;
        }

        // Requirements checklist
        if (reqLength) {
            const validLen = val.length >= 10 && val.length <= 64;
            reqLength.classList.toggle('is-met', validLen);
            reqLength.setAttribute('aria-checked', String(validLen));
        }

        if (reqVariety) {
            const hasVariety = (/\d/.test(val) && /[a-zA-Z]/.test(val)) || /[^a-zA-Z0-9]/.test(val);
            reqVariety.classList.toggle('is-met', hasVariety);
            reqVariety.setAttribute('aria-checked', String(hasVariety));
        }

        // Match confirmation
        if (matchStatus && confirmInput) {
            if (!confirmVal) {
                matchStatus.textContent = '';
                matchStatus.className = 'password-match-badge';
            } else if (val === confirmVal) {
                matchStatus.textContent = '✓ Passwords match';
                matchStatus.className = 'password-match-badge is-matching';
            } else {
                matchStatus.textContent = '✕ Passwords do not match';
                matchStatus.className = 'password-match-badge is-mismatched';
            }
        }
    }

    passwordInput.addEventListener('input', update);
    if (confirmInput) {
        confirmInput.addEventListener('input', update);
    }

    // Run initial update in case browser autofilled
    update();
}

function initFormSubmission() {
    const form = document.querySelector('.account-form');
    if (!form) return;

    form.addEventListener('submit', event => {
        if (!form.checkValidity()) return;

        const button = form.querySelector('.auth-submit-btn');
        if (!button) return;

        // Prevent double submit and show loading state
        button.classList.add('is-submitting');
        const spinner = button.querySelector('.submit-spinner');
        if (spinner) spinner.removeAttribute('hidden');
    });
}

function initAutofocus() {
    // Only autofocus if user hasn't already focused something and there's no error alert stealing focus
    const errorAlert = document.querySelector('.feedback-error');
    if (errorAlert) {
        errorAlert.setAttribute('tabindex', '-1');
        errorAlert.focus();
        return;
    }

    const firstInput = document.querySelector('.account-form input:not([type="hidden"])');
    if (firstInput && !document.activeElement?.matches('input, button, a')) {
        firstInput.focus();
    }
}

