'use strict';

(function () {
    function playChime() {
        try {
            const AudioCtx = window.AudioContext || window.webkitAudioContext;
            if (!AudioCtx) return;
            const ctx = new AudioCtx();
            const now = ctx.currentTime;

            const osc1 = ctx.createOscillator();
            const gain1 = ctx.createGain();
            osc1.type = 'sine';
            osc1.frequency.setValueAtTime(659.25, now);
            gain1.gain.setValueAtTime(0.12, now);
            gain1.gain.exponentialRampToValueAtTime(0.001, now + 0.3);
            osc1.connect(gain1);
            gain1.connect(ctx.destination);
            osc1.start(now);
            osc1.stop(now + 0.3);

            const osc2 = ctx.createOscillator();
            const gain2 = ctx.createGain();
            osc2.type = 'sine';
            osc2.frequency.setValueAtTime(880, now + 0.12);
            gain2.gain.setValueAtTime(0.15, now + 0.12);
            gain2.gain.exponentialRampToValueAtTime(0.001, now + 0.55);
            osc2.connect(gain2);
            gain2.connect(ctx.destination);
            osc2.start(now + 0.12);
            osc2.stop(now + 0.55);
            osc2.addEventListener('ended', () => { void ctx.close(); }, { once: true });
        } catch (e) {
            // Audio context not allowed before user gesture
        }
    }

    function formatTime(seconds) {
        const m = Math.floor(seconds / 60);
        const s = seconds % 60;
        return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
    }

    function initStatusControls() {
        document.querySelectorAll('.card-status-control').forEach(control => {
            const select = control.querySelector('.status-native-select');
            const buttons = control.querySelectorAll('.status-btn');
            const card = control.closest('.workout-card');
            if (!select || !card) return;

            const syncUI = (status) => {
                buttons.forEach(btn => {
                    const match = btn.getAttribute('data-status') === status;
                    btn.classList.toggle('is-active', match);
                    btn.setAttribute('aria-checked', String(match));
                });
                card.classList.toggle('is-status-completed', status === 'COMPLETED');
                card.classList.toggle('is-status-skipped', status === 'SKIPPED');
                recalculateHUD();
            };

            syncUI(select.value || 'PENDING');

            buttons.forEach(btn => {
                btn.addEventListener('click', () => {
                    const status = btn.getAttribute('data-status');
                    select.value = status;
                    select.dispatchEvent(new Event('change', { bubbles: true }));
                    syncUI(status);
                });
            });

            select.addEventListener('change', () => {
                syncUI(select.value);
            });
        });
    }

    function initSteppers(container) {
        const rows = container.matches?.('.card-set-row') ? [container] : container.querySelectorAll('.card-set-row');
        rows.forEach(row => {
            const fieldset = row.closest('.work-sets');
            const stepKg = parseFloat((fieldset && fieldset.dataset.loadStep) || '2.5');

            const weightInput = row.querySelector('.col-weight input');
            const weightDown = row.querySelector('.col-weight .stepper-down');
            const weightUp = row.querySelector('.col-weight .stepper-up');

            if (weightInput && weightDown && weightUp && !weightDown.dataset.initialized) {
                weightDown.dataset.initialized = 'true';
                weightDown.addEventListener('click', (e) => {
                    e.preventDefault();
                    let val = parseFloat(weightInput.value);
                    if (isNaN(val)) val = parseFloat(fieldset?.dataset.targetLoad || '0');
                    val = Math.max(0, Math.round((val - stepKg) * 100) / 100);
                    weightInput.value = val.toFixed(2);
                    weightInput.dispatchEvent(new Event('input', { bubbles: true }));
                });
                weightUp.addEventListener('click', (e) => {
                    e.preventDefault();
                    let val = parseFloat(weightInput.value);
                    if (isNaN(val)) val = parseFloat(fieldset?.dataset.targetLoad || '0');
                    val = Math.min(1000, Math.round((val + stepKg) * 100) / 100);
                    weightInput.value = val.toFixed(2);
                    weightInput.dispatchEvent(new Event('input', { bubbles: true }));
                });
            }

            const repsInput = row.querySelector('.col-reps input');
            const rirInput = row.querySelector('.col-rir input');
            const repsDown = row.querySelector('.col-reps .stepper-down');
            const repsUp = row.querySelector('.col-reps .stepper-up');

            if (repsInput && repsDown && repsUp && !repsDown.dataset.initialized) {
                repsDown.dataset.initialized = 'true';
                repsDown.addEventListener('click', (e) => {
                    e.preventDefault();
                    let val = parseInt(repsInput.value, 10);
                    if (isNaN(val)) val = parseInt(fieldset?.dataset.repMin || '8', 10);
                    val = Math.max(1, val - 1);
                    repsInput.value = val;
                    repsInput.dispatchEvent(new Event('input', { bubbles: true }));
                });
                repsUp.addEventListener('click', (e) => {
                    e.preventDefault();
                    let val = parseInt(repsInput.value, 10);
                    if (isNaN(val)) val = parseInt(fieldset?.dataset.repMin || '8', 10);
                    val = Math.min(100, val + 1);
                    repsInput.value = val;
                    repsInput.dispatchEvent(new Event('input', { bubbles: true }));
                });
            }

            const copyBtn = row.querySelector('.btn-copy-target');
            if (copyBtn && !copyBtn.dataset.initialized) {
                copyBtn.dataset.initialized = 'true';
                copyBtn.addEventListener('click', (e) => {
                    e.preventDefault();
                    if (fieldset && fieldset.dataset.targetLoad && weightInput) {
                        weightInput.value = parseFloat(fieldset.dataset.targetLoad).toFixed(2);
                        weightInput.dispatchEvent(new Event('input', { bubbles: true }));
                        if (repsInput && !repsInput.value) {
                            repsInput.focus();
                        }
                    }
                });
            }

            const checkBtn = row.querySelector('.btn-check-set');
            if (checkBtn && !checkBtn.dataset.initialized) {
                checkBtn.dataset.initialized = 'true';
                checkBtn.addEventListener('click', (e) => {
                    e.preventDefault();
                    const missing = [weightInput, repsInput].find(input => input?.value === '');
                    if (missing) {
                        missing.focus();
                        return;
                    }
                    const invalid = [weightInput, repsInput, rirInput].find(input => input && !input.validity.valid);
                    if (invalid) {
                        invalid.reportValidity();
                        return;
                    }

                    recalculateRowState(row);
                    recalculateHUD();

                    const card = row.closest('.workout-card');
                    if (card) {
                        checkAutoCardCompletion(card);
                        const timer = card.querySelector('.rest-timer-bar');
                        if (timer && timer.startTimer) {
                            timer.startTimer();
                        }
                    }
                });
            }

            const clearBtn = row.querySelector('.btn-clear-set');
            if (clearBtn && !clearBtn.dataset.initialized) {
                clearBtn.dataset.initialized = 'true';
                clearBtn.addEventListener('click', (e) => {
                    e.preventDefault();
                    if (weightInput) weightInput.value = '';
                    if (repsInput) repsInput.value = '';
                    if (rirInput) rirInput.value = '';
                    row.classList.remove('is-completed');
                    recalculateRowState(row);
                    recalculateHUD();
                });
            }

            [weightInput, repsInput, rirInput].forEach(inp => {
                if (inp) {
                    inp.addEventListener('input', () => {
                        recalculateRowState(row);
                        recalculateHUD();
                    });
                }
            });

            recalculateRowState(row);
        });
    }

    function recalculateRowState(row) {
        const weightInput = row.querySelector('.col-weight input');
        const repsInput = row.querySelector('.col-reps input');
        const isFilled = [weightInput, repsInput].every(input => input && input.value !== '' && input.validity.valid);
        row.classList.toggle('is-completed', isFilled && (row.querySelector('.col-rir input')?.validity.valid ?? true));
    }

    function checkAutoCardCompletion(card) {
        const fieldset = card.querySelector('.work-sets');
        if (!fieldset) return;
        const rows = Array.from(fieldset.querySelectorAll('.card-set-row'));
        const allFilled = rows.length > 0 && rows.every(row => row.classList.contains('is-completed'));

        if (allFilled) {
            const statusSelect = card.querySelector('.status-native-select');
            if (statusSelect && statusSelect.value === 'PENDING') {
                statusSelect.value = 'COMPLETED';
                statusSelect.dispatchEvent(new Event('change', { bubbles: true }));
            }
        }
    }

    function initRestTimers() {
        document.querySelectorAll('.rest-timer-bar').forEach(bar => {
            const defaultSec = parseInt(bar.getAttribute('data-default-seconds') || '90', 10);
            let remaining = defaultSec;
            let timerId = null;
            let isRunning = false;
            let deadline = 0;

            const clockEl = bar.querySelector('.timer-clock');
            const fillEl = bar.querySelector('.timer-progress-fill');
            const startBtn = bar.querySelector('.timer-btn-start');
            const startText = startBtn?.querySelector('span');
            const adjustBtn = bar.querySelector('.timer-btn-adjust');
            const resetBtn = bar.querySelector('.timer-btn-reset');

            const updateDisplay = () => {
                if (clockEl) clockEl.textContent = formatTime(remaining);
                if (fillEl) {
                    const pct = defaultSec > 0 ? Math.max(0, Math.min(100, (remaining / defaultSec) * 100)) : 0;
                    fillEl.style.width = `${pct}%`;
                }
            };

            const stopTimer = () => {
                if (timerId) {
                    clearInterval(timerId);
                    timerId = null;
                }
                isRunning = false;
                bar.classList.remove('is-timer-running');
                if (startText) startText.textContent = 'Start Rest';
            };

            const startTimer = () => {
                if (remaining <= 0) remaining = defaultSec;
                if (remaining <= 0 || isRunning) return;
                deadline = Date.now() + remaining * 1000;
                stopTimer();
                isRunning = true;
                bar.classList.add('is-timer-running');
                bar.classList.remove('is-timer-finished');
                if (startText) startText.textContent = 'Pause';
                updateDisplay();

                timerId = setInterval(() => {
                    remaining = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
                    if (remaining <= 0) {
                        remaining = 0;
                        stopTimer();
                        updateDisplay();
                        bar.classList.add('is-timer-finished');
                        playChime();
                        if (navigator.vibrate) {
                            try { navigator.vibrate([120, 80, 120]); } catch (e) { }
                        }
                    } else {
                        updateDisplay();
                    }
                }, 250);
            };

            bar.startTimer = startTimer;
            updateDisplay();

            if (startBtn) {
                startBtn.addEventListener('click', (e) => {
                    e.preventDefault();
                    if (isRunning) {
                        remaining = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
                        stopTimer();
                        updateDisplay();
                    } else {
                        startTimer();
                    }
                });
            }

            if (adjustBtn) {
                adjustBtn.addEventListener('click', (e) => {
                    e.preventDefault();
                    const adjustSec = parseInt(adjustBtn.getAttribute('data-adjust') || '30', 10);
                    remaining += adjustSec;
                    if (isRunning) {
                        deadline += adjustSec * 1000;
                        remaining = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
                    }
                    updateDisplay();
                });
            }

            if (resetBtn) {
                resetBtn.addEventListener('click', (e) => {
                    e.preventDefault();
                    stopTimer();
                    remaining = defaultSec;
                    bar.classList.remove('is-timer-finished');
                    updateDisplay();
                });
            }
        });
    }

    function initAddSetButtons() {
        document.querySelectorAll('.add-set').forEach(button => {
            const card = button.closest('.workout-card');
            const fieldset = card?.querySelector('.work-sets');
            if (!fieldset) return;

            const rows = () => fieldset.querySelectorAll('.card-set-row');
            const updateDisabled = () => { button.disabled = rows().length >= 12; };
            updateDisabled();

            button.addEventListener('click', (e) => {
                e.preventDefault();
                const currentRows = rows();
                const index = currentRows.length;
                if (index >= 12) return;

                const templateRow = currentRows[0];
                if (!templateRow) return;

                const copy = templateRow.cloneNode(true);
                copy.setAttribute('data-set-index', String(index));
                copy.classList.remove('is-completed');
                copy.classList.add('is-new');

                const label = copy.querySelector('.set-label');
                if (label) label.textContent = `Set ${index + 1}`;

                copy.querySelectorAll('input').forEach(input => {
                    input.name = input.name.replace(/\.sets\[\d+\]/, `.sets[${index}]`);
                    if (input.id) {
                        input.id = input.id.replace(/-set-\d+-/, `-set-${index}-`);
                    }
                    const ariaLabel = input.getAttribute('aria-label');
                    if (ariaLabel) {
                        input.setAttribute('aria-label', ariaLabel.replace(/set \d+/, `set ${index + 1}`));
                    }
                    input.value = '';
                });

                copy.querySelectorAll('label[for]').forEach(label => {
                    label.htmlFor = label.htmlFor.replace(/-set-\d+-/, `-set-${index}-`);
                });

                copy.querySelectorAll('[data-initialized]').forEach(el => delete el.dataset.initialized);

                fieldset.append(copy);
                initSteppers(copy);
                updateDisabled();
                recalculateHUD();

                const firstInput = copy.querySelector('input');
                if (firstInput) firstInput.focus();
            });
        });
    }

    function initAutofillButtons() {
        document.querySelectorAll('.autofill-sets').forEach(btn => {
            const card = btn.closest('.workout-card');
            const fieldset = card?.querySelector('.work-sets');
            if (!fieldset || !fieldset.dataset.targetLoad) return;

            btn.addEventListener('click', (e) => {
                e.preventDefault();
                const targetKg = parseFloat(fieldset.dataset.targetLoad).toFixed(2);
                fieldset.querySelectorAll('.card-set-row').forEach(row => {
                    const weightInput = row.querySelector('.col-weight input');
                    if (weightInput && !weightInput.value.trim()) {
                        weightInput.value = targetKg;
                        weightInput.dispatchEvent(new Event('input', { bubbles: true }));
                    }
                });
                recalculateHUD();
            });
        });
    }

    function initQuickJumpPills() {
        document.querySelectorAll('.exercise-pill').forEach(pill => {
            pill.addEventListener('click', (e) => {
                e.preventDefault();
                const targetId = pill.getAttribute('data-target-exercise');
                const targetEl = document.getElementById(targetId);
                if (targetEl) {
                    targetEl.scrollIntoView({ behavior: 'smooth', block: 'start' });
                    targetEl.classList.add('is-highlighted');
                    setTimeout(() => targetEl.classList.remove('is-highlighted'), 1200);
                }
            });
        });
    }

    function recalculateHUD() {
        const cards = Array.from(document.querySelectorAll('.workout-card'));
        const totalCards = cards.length;
        if (totalCards === 0) return;

        let completedCards = 0;
        let totalLoggedSets = 0;
        let totalWorkoutVolume = 0;

        cards.forEach((card, idx) => {
            const statusSelect = card.querySelector('.status-native-select');
            const isCompletedStatus = statusSelect && statusSelect.value === 'COMPLETED';
            const isSkippedStatus = statusSelect && statusSelect.value === 'SKIPPED';

            const rows = Array.from(card.querySelectorAll('.card-set-row'));
            let cardVolume = 0;
            let cardSetsLogged = 0;

            rows.forEach(r => {
                const w = parseFloat(r.querySelector('.col-weight input')?.value);
                const reps = parseInt(r.querySelector('.col-reps input')?.value, 10);
                if (!isNaN(w) && !isNaN(reps) && reps > 0) {
                    cardVolume += (w * reps);
                    cardSetsLogged++;
                }
            });

            const cardVolEl = card.querySelector('.card-volume-val');
            if (cardVolEl) cardVolEl.textContent = Math.round(cardVolume).toLocaleString();

            totalWorkoutVolume += cardVolume;
            totalLoggedSets += cardSetsLogged;

            const isDone = isCompletedStatus || (!isSkippedStatus && rows.length > 0 && cardSetsLogged >= rows.length);
            if (isDone) completedCards++;

            const pill = document.querySelector(`.exercise-pill[data-index="${idx}"]`);
            if (pill) {
                pill.classList.toggle('is-finished', isDone);
            }
        });

        const hudCompleted = document.getElementById('hud-completed-count');
        const hudSets = document.getElementById('hud-sets-count');
        const hudVolume = document.getElementById('hud-volume-count');
        const hudProgress = document.getElementById('hud-progress-fill');

        if (hudCompleted) hudCompleted.textContent = String(completedCards);
        if (hudSets) hudSets.textContent = String(totalLoggedSets);
        if (hudVolume) hudVolume.textContent = Math.round(totalWorkoutVolume).toLocaleString();
        if (hudProgress) {
            const pct = Math.round((completedCards / totalCards) * 100);
            hudProgress.style.width = `${pct}%`;
        }

        const actionLabel = document.getElementById('action-completed-label');
        const actionVol = document.getElementById('action-total-volume');
        if (actionLabel) actionLabel.textContent = `${completedCards} of ${totalCards}`;
        if (actionVol) actionVol.textContent = Math.round(totalWorkoutVolume).toLocaleString();
    }

    function init() {
        initStatusControls();
        initSteppers(document);
        initRestTimers();
        initAddSetButtons();
        initAutofillButtons();
        initQuickJumpPills();
        recalculateHUD();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
