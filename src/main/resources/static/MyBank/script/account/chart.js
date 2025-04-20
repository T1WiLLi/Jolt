import { formatNumber } from '../helper/helper.js'

const AC_VALUE_MAIN = parseFloat(loggedInUser.Account.money['main-account'].replace(/,/g, ''));
const AC_VALUE_SAVE = parseFloat(loggedInUser.Account.money['saving-account'].replace(/,/g, ''));
const AC_VALUE_PLAC = parseFloat(loggedInUser.Account.money['placement-account'].replace(/,/g, ''));

// Generate initial data for all accounts
const mainAccountData = generateRandomValueMain(AC_VALUE_MAIN);
const saveAccountData = generateRandomValueSave(AC_VALUE_SAVE);
const placementAccountData = generateLogValuePlacement(AC_VALUE_PLAC);

function generateRandomValueMain(finalValue) { // For main account
    let data = [];
    for (let i = 0; i < 5; i++) {
        let min = finalValue * 1;
        let max = finalValue * 4.5;
        data.push(parseFloat((Math.random() * (max - min) + min).toFixed(2)));
    }
    data.push(finalValue);
    return data;
}

function generateRandomValueSave(finalValue) {
  let data = [];
  let currentValue = finalValue;

  for (let i = 0; i < 5; i++) {
    let min = currentValue * 0.90;
    let max = currentValue * 1.1;
    let newValue = Math.random() * (max - min) + min;
    data.push(parseFloat(newValue.toFixed(2)));
    currentValue = newValue;
  }

  data.push(finalValue);
  return data;
}

function generateLogValuePlacement(finalValue) {
  let data = [];
  let initialValue = finalValue / Math.pow(1.05, 6); // Calculate the initial value based on the final value and 6-month period

  let growthFactor = Math.pow(finalValue / initialValue, 1 / 6); // Calculate the growth factor

  let currentValue = initialValue;

  for (let i = 0; i < 6; i++) {
    data.push(parseFloat(currentValue.toFixed(2)));
    currentValue *= growthFactor; // Apply the growth factor to get the next value
  }

  data.push(finalValue);
  return data;
}

// Helper function to get the month name from the month index
function getMonthName(monthIndex) {
  var months = [
    'Jan',
    'Feb',
    'Mar',
    'Apr',
    'May',
    'Jun',
    'Jul',
    'Aug',
    'Sep',
    'Oct',
    'Nov',
    'Dec',
  ];
  return months[monthIndex];
}
//Month
var currentDate = new Date();
var currentMonth = currentDate.getMonth();
var categories = [];
for (var i = 5; i >= 0; i--) {
  var prevMonth = (currentMonth - i + 12) % 12;
  categories.push(getMonthName(prevMonth));
}
categories.push(getMonthName(currentMonth));


var currentChartType = 'main';

// Chart options and settings
const options = {
    series: [
        {
            name: 'Money',
            data: mainAccountData,
        },
    ],
    chart: {
        height: 400,
        type: 'bar',
        background: 'var(--background-color)',
        foreColor: 'var(--main-color)',
    },
    fill: {
        colors: ['#54B435'],
        opacity: 0.8,
    },
    plotOptions: {
        bar: {
            borderRadius: 0,
            dataLabels: {
                position: 'top',
                maxItems: 5,
                hideOverflowingLabels: true,
                style: {
                    padding: '0rem 5px 0 5px',
                    colors: ['#FFF'],
                    fontWeight: 'bold',
                    fontFamily: 'var(--general-font)',
                },
            },
        },
    },
    dataLabels: {
        enabled: true,
        offsetY: 10,
        style: {
            fontSize: currentChartType === "placement" ? '12px' : '10px',
            colors: ['white'],
            fontWeight: 'bolder',
        },
        formatter: function (val) {
            return '$' + formatNumber(val);
        },
    },
    xaxis: {
        categories: categories,
        position: 'top',
        reversed: true,
        axisBorder: {
            show: false,
        },
        axisTicks: {
            show: false,
        },
        crosshairs: {
            fill: {
            type: 'gradient',
            gradient: {
                colorFrom: '#D8E3F0',
                colorTo: '#BED1E6',
                stops: [0, 100],
                opacityFrom: 0.4,
                opacityTo: 0.5,
            },
            },
        },
        tooltip: {
            enabled: true,
        },
    },
    yaxis: {
        axisBorder: {
            show: false,
        },
        axisTicks: {
            show: false,
        },
        labels: {
            show: true,
            offsetY: 0,
        },
    },
    title: {
        text: 'Monthly Wallet Balance for the Past 6 Months',
        floating: true,
        offsetY: 380,
        align: 'center',
        style: {
            color: '#444',
            fontSize: '16px',
            fontWeight: 'bold',
            fontFamily: 'var(--general-font)',
        },
    },
};

var chart = new ApexCharts(document.querySelector("#chart"), options);
chart.render();

// Button click event handlers
document.getElementById("current-button").addEventListener("click", function () {
  currentChartType = "main";
  options.series = [{
    name: "Money",
    data: mainAccountData,
  }];
  chart.updateOptions(options);
});

document.getElementById("saving-button").addEventListener("click", function () {
  currentChartType = "saving";
  options.series = [{
    name: "Money",
    data: saveAccountData,
  }];
  chart.updateOptions(options);
});

document.getElementById("placement-button").addEventListener("click", function () {
  currentChartType = "placement";
  options.series = [{
    name: "Money",
    data: placementAccountData,
  }];
  chart.updateOptions(options);
});