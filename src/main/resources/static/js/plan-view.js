'use strict';

/**
 * Interactive behaviors for /plans:
 * - Day filter tabs (switch between "All days" masonry and focused single day views)
 * - Support URL hash deep-links (e.g. #plan-day-123 from the dashboard)
 * - Seamless elimination of empty space
 */
(function() {
    document.addEventListener('click', function(e) {
        var btn = e.target.closest('.day-filter-btn');
        if (!btn) return;
        e.preventDefault();

        var toolbar = document.querySelector('.plan-days-toolbar');
        var grid = document.querySelector('.plan-days-grid');
        if (!grid) return;

        var filter = btn.getAttribute('data-day-filter');
        var isAlreadyActive = btn.classList.contains('is-active') && filter !== 'all';
        var targetFilter = isAlreadyActive ? 'all' : filter;

        // Update active tab buttons
        var buttons = toolbar ? Array.from(toolbar.querySelectorAll('.day-filter-btn')) : [];
        buttons.forEach(function(b) {
            var bFilter = b.getAttribute('data-day-filter');
            var isActive = (bFilter === targetFilter);
            b.classList.toggle('is-active', isActive);
            b.setAttribute('aria-selected', String(isActive));
        });

        // Update day cards in grid
        var cards = Array.from(grid.querySelectorAll('.plan-day-card'));
        if (targetFilter === 'all') {
            grid.classList.remove('is-single-day');
            cards.forEach(function(card) {
                card.classList.remove('is-card-hidden');
                card.removeAttribute('hidden');
                card.style.display = '';
            });
        } else {
            grid.classList.add('is-single-day');
            cards.forEach(function(card) {
                var cardIdx = card.getAttribute('data-day-index');
                var cardId = card.getAttribute('data-day-id');
                var match = (cardIdx === targetFilter) || (cardId === targetFilter);
                if (match) {
                    card.classList.remove('is-card-hidden');
                    card.removeAttribute('hidden');
                    card.style.display = '';
                } else {
                    card.classList.add('is-card-hidden');
                    card.setAttribute('hidden', '');
                    card.style.display = 'none';
                }
            });
        }
    });

    if (window.location.hash) {
        var targetCard = document.querySelector(window.location.hash);
        if (targetCard && targetCard.classList.contains('plan-day-card')) {
            var idx = targetCard.getAttribute('data-day-index');
            if (idx !== null) {
                var targetBtn = document.querySelector('.day-filter-btn[data-day-filter="' + idx + '"]');
                if (targetBtn) targetBtn.click();
            }
        }
    }
})();
