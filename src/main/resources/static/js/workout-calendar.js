'use strict';

(function () {
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    const narrowScreen = window.matchMedia('(max-width: 860px)');

    function animateArrival(element, offset, duration) {
        if (!element || reduceMotion.matches || typeof element.animate !== 'function') return;
        element.getAnimations().forEach(animation => animation.cancel());
        element.animate(
            [
                { opacity: 0.35, transform: offset },
                { opacity: 1, transform: 'none' }
            ],
            { duration, easing: 'cubic-bezier(0.16, 1, 0.3, 1)' }
        );
    }

    function initFullCalendar() {
        const calendar = document.querySelector('#workout-calendar');
        if (!calendar) return;

        const announcement = calendar.querySelector('[data-calendar-status]');
        let view = readView(calendar.querySelector('.calendar-layout'), location.search);
        let visibleSearch = location.search;
        let request;

        function readView(layout, search) {
            const days = [...layout.querySelectorAll('[data-calendar-day]')];
            return {
                layout,
                search,
                month: layout.dataset.calendarMonth,
                initialDate: layout.dataset.calendarSelection,
                selectedDate: layout.dataset.calendarSelection,
                days,
                dayByDate: new Map(days.map(link => [link.dataset.calendarDay, link])),
                sessionDays: days.filter(link => link.dataset.hasSession === 'true'),
                panels: new Map(
                    [...layout.querySelectorAll('[data-calendar-panel]')].map(panel => [panel.dataset.calendarPanel, panel])
                ),
                emptyPanel: layout.querySelector('[data-calendar-empty-panel]'),
                detail: layout.querySelector('[data-calendar-detail]'),
                logControls: layout.querySelector('[data-calendar-log-controls]')
            };
        }

        function adjacentLog(step) {
            return step === 'earlier'
                ? view.sessionDays.findLast(link => link.dataset.calendarDay < view.selectedDate)
                : view.sessionDays.find(link => link.dataset.calendarDay > view.selectedDate);
        }

        function updateLogControls() {
            if (!view.logControls) return;
            view.logControls.hidden = view.sessionDays.length === 0;
            for (const button of view.logControls.querySelectorAll('button[data-calendar-log-step]')) {
                button.disabled = !adjacentLog(button.dataset.calendarLogStep);
            }
        }

        function updateEmptyPanel(date, link) {
            if (!view.emptyPanel) return;
            const dateHeading = view.emptyPanel.querySelector('[data-empty-date-label]');
            const todayBadge = view.emptyPanel.querySelector('[data-empty-today-badge]');
            const label = link?.dataset.calendarLabel || date;
            if (dateHeading) dateHeading.textContent = label;
            if (todayBadge) todayBadge.hidden = link?.dataset.isToday !== 'true';

            for (const quickLink of view.emptyPanel.querySelectorAll('[data-quick-log-link]')) {
                const targetUrl = new URL(quickLink.href, location.origin);
                targetUrl.searchParams.set('date', date);
                quickLink.href = targetUrl.pathname + targetUrl.search;
            }
        }

        function selectDate(date) {
            const link = view.dayByDate.get(date);
            if (!link || date === view.selectedDate) return;

            for (const panel of view.panels.values()) {
                panel.getAnimations().forEach(animation => animation.cancel());
                panel.hidden = true;
            }
            if (view.emptyPanel) {
                view.emptyPanel.getAnimations().forEach(animation => animation.cancel());
                view.emptyPanel.hidden = true;
            }

            view.selectedDate = date;
            for (const dayLink of view.days) {
                if (dayLink.dataset.calendarDay === date) {
                    dayLink.setAttribute('aria-current', 'date');
                } else {
                    dayLink.removeAttribute('aria-current');
                }
            }

            const targetPanel = view.panels.get(date);
            if (targetPanel) {
                targetPanel.hidden = false;
                animateArrival(targetPanel, 'translateY(8px)', 220);
                if (announcement) {
                    announcement.textContent = targetPanel.querySelector('h3')?.textContent || date;
                }
            } else if (view.emptyPanel) {
                updateEmptyPanel(date, link);
                view.emptyPanel.hidden = false;
                animateArrival(view.emptyPanel, 'translateY(8px)', 220);
                if (announcement) {
                    announcement.textContent = link.dataset.calendarLabel || date;
                }
            }

            updateLogControls();
        }

        function cancelLoad() {
            request?.abort();
            request = undefined;
            view.layout?.removeAttribute('aria-busy');
        }

        function openDay(link) {
            if (!link) return;
            cancelLoad();
            if (link.href !== location.href) history.pushState(null, '', link.href);
            visibleSearch = location.search;
            selectDate(link.dataset.calendarDay);
        }

        async function openMonth(url, control) {
            cancelLoad();
            const current = new AbortController();
            request = current;
            view.layout.setAttribute('aria-busy', 'true');
            try {
                const response = await fetch(url, { signal: current.signal });
                if (!response.ok) throw new Error('Calendar request failed');
                const page = new DOMParser().parseFromString(await response.text(), 'text/html');
                const layout = page.querySelector('#workout-calendar .calendar-layout');
                if (current.signal.aborted) return;
                if (!layout) throw new Error('Calendar missing');

                const previousMonth = view.month;
                const restoreFocus = control
                    ? document.activeElement === control
                    : view.layout.contains(document.activeElement);
                if (control) history.pushState(null, '', url);
                view.layout.replaceWith(layout);
                view = readView(layout, url.search);
                visibleSearch = url.search;
                updateLogControls();

                const heading = layout.querySelector('#calendar-month-label');
                if (view.month !== previousMonth) {
                    const offset = view.month > previousMonth ? 18 : -18;
                    animateArrival(layout.querySelector('.calendar-grid'), `translateX(${offset}px)`, 260);
                    animateArrival(layout.querySelector('.calendar-detail-panel'), 'translateY(8px)', 260);
                }
                if (announcement && heading) announcement.textContent = heading.textContent;
                if (restoreFocus) {
                    const equivalent = control && [...layout.querySelectorAll('[data-calendar-month-link]')]
                        .find(link => link.dataset.calendarMonthLink === control.dataset.calendarMonthLink);
                    (equivalent || heading)?.focus({ preventScroll: true });
                }
            } catch (error) {
                if (!current.signal.aborted) {
                    if (control) location.assign(url);
                    else location.reload();
                }
            } finally {
                if (request === current) cancelLoad();
            }
        }

        calendar.addEventListener('click', event => {
            if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;

            const link = event.target.closest('[data-calendar-day], [data-calendar-month-link]');
            if (link) {
                event.preventDefault();
                if (link.hasAttribute('data-calendar-month-link')) {
                    openMonth(new URL(link.href), link);
                } else {
                    openDay(link);
                    if (event.detail > 0 && narrowScreen.matches && view.detail) {
                        view.detail.focus({ preventScroll: true });
                        view.detail.scrollIntoView({
                            block: 'nearest',
                            behavior: reduceMotion.matches ? 'instant' : 'smooth'
                        });
                    }
                }
                return;
            }

            const step = event.target.closest('[data-calendar-log-step]');
            if (!step || step.disabled) return;
            const focused = document.activeElement === step;
            const targetLink = adjacentLog(step.dataset.calendarLogStep);
            if (!targetLink) return;
            openDay(targetLink);
            if (focused && step.disabled) {
                view.logControls?.querySelector('button:not(:disabled)')?.focus({ preventScroll: true });
            }
        });

        calendar.addEventListener('keydown', event => {
            const link = event.target.closest('[data-calendar-day]');
            if (!link || event.altKey || event.ctrlKey || event.metaKey || event.shiftKey) return;

            let next;
            const idx = view.days.indexOf(link);
            if (event.key === 'ArrowLeft') next = view.days[idx - 1];
            else if (event.key === 'ArrowRight') next = view.days[idx + 1];
            else if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
                const current = new Date(link.dataset.calendarDay + 'T00:00:00Z');
                current.setUTCDate(current.getUTCDate() + (event.key === 'ArrowUp' ? -7 : 7));
                next = view.dayByDate.get(current.toISOString().slice(0, 10));
            } else if (event.key === 'Home') next = view.days[0];
            else if (event.key === 'End') next = view.days[view.days.length - 1];

            if (!next) return;
            event.preventDefault();
            openDay(next);
            next.focus();
        });

        window.addEventListener('popstate', () => {
            cancelLoad();
            const url = new URL(location.href);
            if (url.search === visibleSearch) return;

            const date = url.searchParams.get('date');
            const requestedMonth = url.searchParams.get('month') || date?.slice(0, 7) || view.layout.dataset.calendarCurrentMonth;
            if (url.search === view.search || (requestedMonth === view.month && view.dayByDate.has(date))) {
                selectDate(view.dayByDate.has(date) ? date : view.initialDate);
                visibleSearch = url.search;
            } else {
                openMonth(url);
            }
        });

        updateLogControls();
    }

    function initDashboardMiniCalendar() {
        const mini = document.querySelector('[data-mini-calendar]');
        if (!mini) return;
        const previewTitle = mini.querySelector('[data-mini-preview-title]');
        const previewMeta = mini.querySelector('[data-mini-preview-meta]');
        const previewStatus = mini.querySelector('[data-mini-preview-status]');
        if (!previewTitle || !previewMeta || !previewStatus) return;

        const defaultState = {
            title: previewTitle.textContent,
            meta: previewMeta.textContent,
            status: previewStatus.textContent,
            tone: previewStatus.dataset.tone || 'neutral'
        };

        function showDay(cell) {
            previewTitle.textContent = cell.dataset.miniLabel || defaultState.title;
            previewMeta.textContent = cell.dataset.miniDetail || defaultState.meta;
            previewStatus.textContent = cell.dataset.miniBadge || '';
            previewStatus.dataset.tone = cell.dataset.miniTone || 'neutral';
            previewStatus.hidden = !previewStatus.textContent;
        }

        function resetDay() {
            previewTitle.textContent = defaultState.title;
            previewMeta.textContent = defaultState.meta;
            previewStatus.textContent = defaultState.status;
            previewStatus.dataset.tone = defaultState.tone;
            previewStatus.hidden = !defaultState.status;
        }

        mini.addEventListener('pointerenter', event => {
            const cell = event.target.closest('[data-mini-day]');
            if (cell) showDay(cell);
        }, true);

        mini.addEventListener('focusin', event => {
            const cell = event.target.closest('[data-mini-day]');
            if (cell) showDay(cell);
        });

        mini.addEventListener('pointerleave', resetDay);
        mini.addEventListener('focusout', event => {
            if (!mini.contains(event.relatedTarget)) resetDay();
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            initFullCalendar();
            initDashboardMiniCalendar();
        });
    } else {
        initFullCalendar();
        initDashboardMiniCalendar();
    }
})();
