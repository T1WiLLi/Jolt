const cards = document.querySelectorAll('[data-item]');

setTimeout(() => {
    for (const card of cards) {
        card.setAttribute('data-aos-delay', '');
    }
}, 1500);