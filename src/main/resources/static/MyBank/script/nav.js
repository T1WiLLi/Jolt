import { qs, qsAll } from './helper/helper.js';

const hamburger = qs('.hamburger');
const mobile_menu = qs('[data-mobile]');
const signoutButtons = document.querySelectorAll('[data-sign_out]');

hamburger.addEventListener('click', function () {
    this.classList.toggle('is-active');
    mobile_menu.classList.toggle('is-open');
});

signoutButtons.forEach(button => {
  button.addEventListener('click', () => {
    window.location.href = "connexion.html";
    localStorage.clear();
  });
});