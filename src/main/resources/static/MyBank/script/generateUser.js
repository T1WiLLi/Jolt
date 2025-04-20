import { users, usersFirstNames, usersLastNames, provinces, cities, streetNames } from "./data/user.js";
import { formatNumber, padNumber } from "./helper/helper.js";

class User {
    constructor(id, lastName, firstName, age, dob, email, cellphone, money, username, password, adress) {
        this.id = id;
        this.name = {
            lastname: lastName,
            firstname: firstName,
        };
        this["user-info"] = {
            age: age,
            DOB: dob,
            email: email,
            cellphone: cellphone,
            country: adress[0],
            region: adress[1],
            city: adress[2],
            adress: adress[3],
            ZIP: adress[4],
        };
        this.Account = {
            money: {
                "main-account": money[0],
                "saving-account": money[1],
                "placement-account": money[2],
            }
        };
        this["user-private"] = {
            username: username,
            password: password,
        };
    }
}

class UserGenerator {
    static generateRandomUser() {
        let id;
        do {
            id = this.generateID();
        } while (users.some(user => user.id === id))
        const lastname = this.getRandomLastName();
        const firstname = this.getRandomFirstName();
        const dob = this.getRandomDOB();
        const age = this.getRandomAge(dob);
        const email = this.getRandomEmail(firstname, lastname);
        const cellphone = this.generatePhoneNumber();
        const money = this.getRandomMoney();
        const username = this.getRandomUsername(firstname, lastname);
        const password = this.getRandomPassword();
        const adress = this.generateCanadianAdress();

        return new User(id, lastname, firstname, age, dob, email, cellphone, money, username, password, adress);
    }

    static generateID(){
        const letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        const numbers = "0123456789";
        let id = "";
        for (let i = 0; i < 12; i++) {
            if (i % 2 === 0) {
                id += letters[Math.floor(Math.random() * letters.length)];
            } else {
                id += numbers[Math.floor(Math.random() * numbers.length)];
            }
        }
        return id;
    }

    static getRandomLastName() {
        const lastNames = usersLastNames;
        return lastNames[Math.floor(Math.random() * lastNames.length)];
    }

    static getRandomFirstName() {
        const firstNames = usersFirstNames;
        return firstNames[Math.floor(Math.random() * firstNames.length)];
    }

    static getRandomDOB() {
        const currentDate = new Date();
        const maxAge = 90;
        const minAge = 14;
        const maxYear = currentDate.getFullYear() - minAge;
        const minYear = maxYear - maxAge;
        const year = Math.floor(Math.random() * (maxYear - minYear + 1)) + minYear;
        const month = Math.floor(Math.random() * 12) + 1;
        const day = Math.floor(Math.random() * 28) + 1;
        const dob = new Date(year, month - 1, day);
        return [padNumber(dob.getDate()), padNumber(dob.getMonth() + 1), padNumber(dob.getFullYear())];
    }

    static getRandomAge(dob) {
        const currentDate = new Date();
        const dateOfBirth = new Date(`${dob[2]}-${dob[1]}-${dob[0]}`);
        const diff = currentDate.getTime() - dateOfBirth.getTime();
        const ageInMs = new Date(diff);
        const age = Math.abs(ageInMs.getUTCFullYear() - 1970);
        return age;
    }



    static getRandomEmail(firstname, lastname) {
        const domains = ['gmail.com', 'yahoo.com', 'hotmail.com', 'outlook.com'];
        const randomDomain = domains[Math.floor(Math.random() * domains.length)];
        return `${firstname.toLowerCase()}.${lastname.toLowerCase()}@${randomDomain}`;
    }

    static generatePhoneNumber() {
        const areaCodes = ["263", "367", "418", "438", "450", "468", "514", "579", "581", "819", "873"];
        const areaCode = areaCodes[Math.floor(Math.random() * areaCodes.length)];
        const middleDigits = Math.floor(Math.random() * 1000).toString().padStart(3, "0");
        const endDigits = Math.floor(Math.random() * 10000).toString().padStart(4, "0");
        const phoneNumber = `${areaCode}-${middleDigits}-${endDigits}`;
        return phoneNumber.toString();
    }


    static getRandomMoney() {
        const mainAccount = Math.floor(Math.random() * 10000) + 100;
        const savingAccount = Math.floor(Math.random() * 5000) + 10;
        const placementAccount = Math.floor(Math.random() * 100000) + 1000;
        return [formatNumber(mainAccount), formatNumber(savingAccount), formatNumber(placementAccount)];
    }

    static getRandomUsername(firstname, lastname) {
        const numbers = Math.floor(Math.random() * 10000);
        return `${firstname.toLowerCase()}.${lastname.toLowerCase()}${numbers}`;
    }

    static getRandomPassword() {
        const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
        let password = '';
        for (let i = 0; i < 8; i++) {
            password += chars[Math.floor(Math.random() * chars.length)];
        }
        return password;
    }

    static generateCanadianAdress() {
        const country = 'Canada';
        const province = Object.values(provinces)[Math.floor(Math.random() * Object.values(provinces).length)];
        const cityKey = Object.keys(provinces).find(key => provinces[key] === province);
        const citiesInProvince = cities[cityKey];
        const city = citiesInProvince[Math.floor(Math.random() * citiesInProvince.length)];
        const streetNumber = Math.floor(Math.random() * 9999) + 1;
        const streetName = streetNames[Math.floor(Math.random() * streetNames.length)];
        const postalCode = this.generateCanadianPostalCode();

        return [country, province, city, (streetNumber + " " + streetName).toString(), postalCode];
    }

    static generateCanadianPostalCode() {
        const postalCodeLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        const postalCodeNumbers = "0123456789";
        
        let postalCode = "";

        postalCode += postalCodeLetters.charAt(Math.floor(Math.random() * postalCodeLetters.length));
        postalCode += postalCodeNumbers.charAt(Math.floor(Math.random() * postalCodeNumbers.length));
        postalCode += postalCodeLetters.charAt(Math.floor(Math.random() * postalCodeLetters.length));
        
        postalCode += postalCodeNumbers.charAt(Math.floor(Math.random() * postalCodeNumbers.length));
        postalCode += postalCodeLetters.charAt(Math.floor(Math.random() * postalCodeLetters.length));
        postalCode += postalCodeNumbers.charAt(Math.floor(Math.random() * postalCodeNumbers.length));
        
        postalCode = postalCode.toUpperCase().replace(/(.{3})/, "$1 ");
        return postalCode;
    }
}

if(users.length < 100) {
    for(let i = 0; i < 100; i++){
        const newUser = UserGenerator.generateRandomUser();
        users.push(newUser);
    }
    console.log(users);
}