const weekOptions = document.querySelector(".home-week-options");
const weekDays = document.querySelectorAll(".home-day");
const weekSummary = document.querySelector(".home-week-summary span");
const dayList = new Intl.ListFormat("en", { type: "conjunction" });

function showSampleWeek(option) {
    const trainingDays = new Set(option.dataset.days.split(" ").map(Number));
    const dayNames = [];

    weekDays.forEach((day, index) => {
        const isTraining = trainingDays.has(index);
        day.classList.toggle("is-training", isTraining);
        if (isTraining) {
            dayNames.push(day.dataset.weekday);
        }
    });

    weekSummary.textContent = dayList.format(dayNames);
}

weekOptions.addEventListener("change", (event) => showSampleWeek(event.target));
window.addEventListener("pageshow", () => showSampleWeek(weekOptions.querySelector(":checked")));
weekOptions.hidden = false;
