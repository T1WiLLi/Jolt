export function formatNumber(number) {
    return number.toLocaleString('en-US', { maximumFractionDigits: 0 });
}

export function padNumber(number) {
    return number.toString().padStart(2, '0');
}

export function qs(selected) { //Easy use of the queryselector
    return document.querySelector(selected);
}

export function qsAll(selected) {
    return document.querySelectorAll(selected);
}

export function getRandomNumber(min, max) {
    return Math.floor(Math.random() * (max - min + 1) + min);
}