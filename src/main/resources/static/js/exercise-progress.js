'use strict';

const progressCharts = document.querySelector('[data-progress-charts]');
const performances = Array.from(document.querySelectorAll('[data-progress-history] tr')).reverse();
if (progressCharts && performances.length && typeof Chart !== 'undefined') {
    const dateFormat = new Intl.DateTimeFormat('en-GB', {day: 'numeric', month: 'short', timeZone: 'UTC'});
    const labels = performances.map(row => dateFormat.format(new Date(row.dataset.date)));
    const styles = getComputedStyle(document.documentElement);
    const muted = styles.getPropertyValue('--muted').trim();
    const accent = styles.getPropertyValue('--accent').trim();
    const grid = styles.getPropertyValue('--line').trim();
    const latest = performances.findLast(row => row.hasAttribute('data-reps'));

    function drawChart(metric, type, values, unit) {
        if (!values.some(value => value !== null)) return;

        const figure = progressCharts.querySelector('[data-chart="' + metric + '"]');
        const canvas = figure.querySelector('canvas');
        if (!canvas.getContext('2d')) return;

        progressCharts.hidden = false;
        figure.hidden = false;
        new Chart(canvas, {
            type,
            data: {
                labels,
                datasets: [{
                    data: values,
                    backgroundColor: accent,
                    borderColor: accent,
                    borderWidth: 2,
                    pointRadius: 4,
                    maxBarThickness: 34,
                    spanGaps: false
                }]
            },
            options: {
                animation: false,
                maintainAspectRatio: false,
                plugins: {
                    legend: {display: false},
                    tooltip: {
                        callbacks: {
                            title: ([item]) => {
                                const row = performances[item.dataIndex];
                                return row.dataset.date + ' · Workout ' + row.dataset.session;
                            },
                            label: item => item.formattedValue + ' ' + unit
                        }
                    }
                },
                scales: {
                    x: {ticks: {color: muted, maxRotation: 0, maxTicksLimit: 5}, grid: {display: false}},
                    y: {beginAtZero: true, ticks: {color: muted, maxTicksLimit: 5, precision: metric === 'reps' ? 0 : undefined},
                        grid: {color: grid}}
                }
            }
        });
    }

    if (latest?.hasAttribute('data-load')) {
        const unit = latest.dataset.convention === 'PER_HAND_EXTERNAL' ? 'kg per hand' : 'kg total';
        progressCharts.querySelector('[data-load-unit]').textContent = '(' + unit + ')';
        const loads = performances.map(row => row.dataset.convention === latest.dataset.convention
            && row.hasAttribute('data-load') ? Number(row.dataset.load) : null);
        drawChart('load', 'line', loads, unit);
    }
    drawChart('reps', 'bar', performances.map(row =>
        row.hasAttribute('data-reps') ? Number(row.dataset.reps) : null), 'reps');
}
