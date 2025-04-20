//Home page JS 

const promo = document.querySelector('[data-promo]');
const promoClose = document.querySelector('#close-button-promo');

function showPromo() {
    promo.style.opacity = '1';
    console.log("Promo is displayed");
}

function hidePromo() {
    promo.style.opacity = '0';
    console.log("Promo is no longer displayed");
}

function isDisplay() {
    if(promo.style.opacity !== '1') {
        showPromo();
    }
    return;
}

setInterval(isDisplay, 10000);

promoClose.addEventListener('click', function() {
    promoClose.preventDefault();
    hidePromo();
});