import { users } from "./data/user.js";
import { qs } from "./helper/helper.js";

class UserLogin { //Handle user connexion to the website. 
    constructor() {
        this.form = qs('[data-form]');
        this.usernameInput = qs('[data-username]');
        this.passwordInput = qs('[data-password]');

        // Add event listener to form submit event
        this.form.addEventListener('submit', this.submitHandler.bind(this));
    }

    submitHandler(event) {
        event.preventDefault();
        const enteredUsername = this.usernameInput.value;
        const enteredPassword = this.passwordInput.value;

        const user = users.find((user) => {
            return user["user-private"].username === enteredUsername && user["user-private"].password === enteredPassword;
        });

        if (user) {
            console.log('Now login as ' + user.name.firstname + " " + user.name.lastname);
            localStorage.setItem('loggedInUser', JSON.stringify(user)); // store user object in localStorage
            window.location.href = "index.html";
        } else {
            const container = qs(".content-container");
            let darker_background = qs('[data-dark]');
            let alert = document.createElement('div');

            darker_background.classList.add("active");
            console.log('Authentication failed');
            alert.classList.add('alert');
            alert.innerHTML = `
                    <div class="card">
                        <div class="card-body">
                            <h5 class="card-title">Authentification failed</h5>
                            <p class="card-text">The username and password does not match-up</p>
                            <button id="alert-btn" class="btn btn-close">Close</button>
                        </div>
                    </div>`;

            container.appendChild(alert);

            const button = qs(".btn-close");
            button.addEventListener('click', function(){
                alert.remove("alert");
                darker_background.classList.remove('active');
            });
        }
    }
}
new UserLogin();