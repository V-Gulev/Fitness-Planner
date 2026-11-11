import {createMuscleModel} from './muscle-model.js';

const explorer = document.querySelector('[data-muscle-explorer]');
const form = document.querySelector('[data-library-filters]');
const muscleField = form.elements.muscle;
const stageLabel = explorer.querySelector('[data-stage-label]');
const modelHelp = explorer.querySelector('[data-model-help]');
const resultsLink = explorer.querySelector('[data-muscle-results]');
const regionIndex = explorer.querySelector('[data-region-index]');
const muscleButtons = [...explorer.querySelectorAll('[data-muscle]')];
const muscleByKey = new Map(muscleButtons.map(button => [button.dataset.muscle, button]));
const viewButtons = [...explorer.querySelectorAll('[data-model-view]')];
const detailButton = explorer.querySelector('[data-model-view="detail"]');
const status = document.querySelector('[data-library-status]');
let results = document.querySelector('#library-results');
let selectedMuscle = muscleField.value;
let modelView = 'front';
let historySearch = location.search;
let request;

const model = createMuscleModel(explorer.querySelector('canvas'), groupOf, showMuscle, chooseFromModel, () => markView(''));
if (model) {
    explorer.hidden = false;
    updateSelection(selectedMuscle, true);
    updateResultsLink();
}

function showMuscle(key) {
    const label = muscleByKey.get(key || selectedMuscle);
    stageLabel.textContent = label?.textContent || key || 'All muscles';
}

function groupOf(key) {
    return muscleByKey.get(key)?.dataset.group || key;
}

function markView(view) {
    for (const button of viewButtons) {
        button.setAttribute('aria-pressed', String(button.dataset.modelView === view));
    }
}

function setView(view) {
    modelView = view;
    model?.setView(view, groupOf(selectedMuscle) || 'CHEST', selectedMuscle);
    markView(view);
    modelHelp.textContent = view === 'detail'
        ? 'Drag to rotate. Select a region to see its exercises.'
        : view === 'skeleton'
        ? 'Drag to rotate. Viewing skeletal framework.'
        : 'Drag to rotate. Select a muscle group to see its regions.';
    updateRegionIndex();
}

function updateRegionIndex() {
    const group = groupOf(selectedMuscle) || 'CHEST';
    const label = muscleByKey.get(group)?.textContent || 'Chest';
    regionIndex.hidden = modelView !== 'detail' && !selectedMuscle;
    regionIndex.setAttribute('aria-label', `${label} regions`);
    detailButton.textContent = selectedMuscle ? 'Detail' : 'Chest detail';
    detailButton.setAttribute('aria-label', `${label} detail`);
    for (const button of regionIndex.querySelectorAll('[data-group]')) {
        button.hidden = button.dataset.group !== group;
    }
}

function updateSelection(key, orient) {
    selectedMuscle = key;
    for (const button of muscleButtons) {
        button.setAttribute('aria-pressed', String(button.dataset.muscle === key));
    }
    model?.select(key);
    showMuscle('');
    if (orient) setView(key ? 'detail' : 'front');
    else updateRegionIndex();
}

function chooseFromModel(key) {
    muscleField.value = key;
    loadResults(filterUrl(), {orient: modelView !== 'detail' || groupOf(key) !== (groupOf(selectedMuscle) || 'CHEST')});
}

explorer.addEventListener('click', event => {
    const button = event.target.closest('button');
    if (!button) return;
    if (button.dataset.modelView) {
        setView(button.dataset.modelView);
        return;
    }
    muscleField.value = button.dataset.muscle;
    loadResults(filterUrl());
});

function filterUrl() {
    const url = new URL(form.action);
    for (const [name, value] of new FormData(form)) {
        if (value.trim()) url.searchParams.set(name, value.trim());
    }
    return url;
}

function fillFilters(url) {
    for (const name of ['q', 'muscle', 'movement']) {
        form.elements[name].value = url.searchParams.get(name) || '';
    }
}

function updateResultsLink() {
    const count = results.querySelector('[data-results-count]').textContent;
    resultsLink.textContent = `View ${count}`;
    return count;
}

async function loadResults(url, {push = true, focus = false, orient = true} = {}) {
    request?.abort();
    const current = new AbortController();
    request = current;
    results.setAttribute('aria-busy', 'true');
    status.textContent = 'Loading exercises';
    try {
        const response = await fetch(url, {signal: current.signal});
        if (!response.ok) throw new Error('Library request failed');
        const next = new DOMParser().parseFromString(await response.text(), 'text/html').querySelector('#library-results');
        if (!next) throw new Error('Library results missing');
        if (current.signal.aborted) return;
        results.replaceWith(next);
        results = next;
        form.querySelector('.filter-errors')?.remove();
        if (push && (url.pathname !== location.pathname || url.search !== location.search)) {
            history.pushState(null, '', url);
            historySearch = url.search;
        }
        const key = url.searchParams.get('muscle') || '';
        updateSelection(key, orient && (key !== selectedMuscle || (key && modelView !== 'detail')));
        const title = results.querySelector('h2').textContent;
        const count = updateResultsLink();
        const range = results.querySelector('[data-results-range]');
        status.textContent = range ? `${title}. ${range.textContent}.` : `${title}. ${count} found.`;
        if (focus) {
            results.focus({preventScroll: true});
            results.scrollIntoView({behavior: 'instant'});
        }
    } catch (error) {
        if (!current.signal.aborted) location.assign(url);
    } finally {
        if (request === current) {
            results.removeAttribute('aria-busy');
            request = undefined;
        }
    }
}

form.addEventListener('submit', event => {
    event.preventDefault();
    loadResults(filterUrl());
});

document.addEventListener('click', event => {
    const link = event.target.closest('[data-clear-filters], [data-library-page]');
    if (!link || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    event.preventDefault();
    const url = new URL(link.href);
    const reset = link.hasAttribute('data-clear-filters');
    if (reset) fillFilters(url);
    loadResults(url, {focus: true, orient: reset});
});

window.addEventListener('popstate', () => {
    const url = new URL(location.href);
    if (url.search === historySearch) return;
    historySearch = url.search;
    fillFilters(url);
    loadResults(url, {push: false});
});
