import data from '../data/service.js'
const services = data; 

class transactionTemplate {
    getTemplate(transactionMonth, transactionDate, serviceURL, serviceName, serviceDesc, servicePrice, color, serviceHour) {
        let html = `
        <li class="transaction" data-transaction>
            <ul class="transaction-content-container">
                <li>
                    <h4 data-serviceMonth">${transactionMonth}</h4>
                    <h4 data-serviceDate>${transactionDate}</h4>
                </li>
                <li>
                    <img width="40px" data-serviceImage src="${serviceURL}" alt="">
                </li>
                <li>
                    <h4 data-serviceName>${serviceName}</h4>
                    <h3 data-serviceDesc>${serviceDesc}</h3>
                </li>
                <li>
                    <h4 style="color:${color};" data-servicePrice>${servicePrice}</h4>
                    <h3>${serviceHour}</h3>
                </li>
            </ul>
        </li>
        `;
        return html;
    }
}

class generateTransaction {
    constructor() {
        this.transactionList = document.querySelector('[data-transaction-list]');
        this.generatedDates = new Map();
    }

    generateDateList() {
        const currentDate = new Date();
        const dateList = [];

        for (let i = 5; i >= 0; i--) {
            const month = currentDate.getMonth() - i;
            const year = currentDate.getFullYear();
            const lastDay = new Date(year, month + 1, 0).getDate();

            for (let j = 0; j < 2; j++) {
                const randomDay = Math.floor(Math.random() * (lastDay - 1)) + 1;
                const transactionDate = new Date(year, month, randomDay);

                if (transactionDate <= currentDate) {
                    dateList.push({ month, day: randomDay });
                }
            }
        }

        return dateList;
    }

    getRandomService() {
        return services[Math.floor(Math.random() * services.length)];
    }

    getRandomHour() {
        const hour = Math.floor(Math.random() * 24).toString().padStart(2, '0');
        const min = Math.floor(Math.random() * 60).toString().padStart(2, '0');
        return `${hour}:${min}${parseInt(hour) >= 12 ? 'PM' : 'AM'}`;
    }

    getRandomPrice() {
        let min = 2;
        let max = 100;
        return (Math.random() * (max - min) + min).toFixed(2);
    }

    generateRandomTransaction() {
    const dateList = this.generateDateList();

    const transactions = dateList.map(({ month, day }) => {
        const currentDate = new Date();
        currentDate.setMonth(month);
        currentDate.setDate(day);

        const transactionMonth = currentDate.toLocaleString('default', { month: 'short' });
        const transactionDate = currentDate.toLocaleString('en-US', { day: '2-digit' });
        const { name, description, imageURL, type } = this.getRandomService();
        const servicePrice = this.getRandomPrice();
        const serviceHour = this.getRandomHour();
        const color = type === 'transfer' ? 'green' : 'red';

        const template = new transactionTemplate;
        return {
            date: currentDate.getTime(),
            html: template.getTemplate(
                transactionMonth.substring(0, 3) + '.',
                transactionDate,
                imageURL,
                name,
                description,
                (color === 'green' ? '+':'-') + '$' + servicePrice,
                color,
                serviceHour
            ),
        };
    });

    const sortedTransactions = transactions.sort((b, a) => a.date - b.date);
    const htmlList = sortedTransactions.map((transaction) => transaction.html);

    this.transactionList.innerHTML = htmlList.join('');
    }
}
const transactionGenerator = new generateTransaction();
transactionGenerator.generateRandomTransaction();


transactionGenerator.getRandomPrice();