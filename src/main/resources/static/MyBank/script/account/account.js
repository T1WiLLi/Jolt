import { dynamicLoader } from "../dynamicLoading/dynamicLoader.js";
import { dashboard } from "../dynamicLoading/templates.js";

const loggedInUser = JSON.parse(localStorage.getItem('loggedInUser'));
console.log(loggedInUser);

let dashboardHTML = new dashboard();
let dash = new dynamicLoader(document.querySelector('#dashboard_parent'), dashboardHTML.getHtml(), dashboardHTML.getScript());

const button1 = document.querySelector('#side-nav_1');
const button2 = document.querySelector('#side-nav_2');

button1.addEventListener('click', () => {
    dash.show();
});

button2.addEventListener('click', () => {
    dash.hide();
});