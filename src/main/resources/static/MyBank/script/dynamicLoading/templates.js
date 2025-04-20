export class dashboard {
    getHtml() {
        return `
            <section class="account-display">
                <div class="account-left">
                    <div class="boxes">
                        <div class="box-item">
                            <i class="fas fa-circle-info"></i>
                            <div class="box-content">
                                <div class="account-name">Current</div>
                                <i class="fas fa-wallet"></i>
                                <h5 class="box-balance">Total Balance</h5>
                                <span data-current>$0</span>
                            </div>
                        </div>
                        <div class="box-item">
                            <i class="fa-solid fa-circle-info"></i>
                            <div class="box-content">
                                <div class="account-name">Savings</div>
                                <i class="fa-solid fa-piggy-bank"></i>
                                <h5 class="box-balance">Total Balance</h5>
                                <span data-saving>$0</span>
                            </div>
                        </div>
                        <div class="box-item">
                            <i class="fa-solid fa-circle-info"></i>
                            <div class="box-content">
                                <div class="account-name">Placement</div>
                                <i class="fa-solid fa-money-bill-trend-up"></i>
                                <h5 class="box-balance">Total Balance</h5>
                                <span data-placement>$0</span>
                            </div>
                        </div>
                    </div>
                    <div class="chart">
                        <div class="buttons-list">
                            <button id="current-button" class="btn">Current</button>
                            <button id="saving-button" class="btn">Saving</button>
                            <button id="placement-button" class="btn">Placement</button>
                        </div>
                        <div id="chart"></div>
                    </div>
                </div>
                <div class="account-right">
                    <div class="transaction-short">
                        <h2>Transaction List</h2>
                        <ul data-transaction-list id="trans-list"></ul>
                    </div>
                </div>
            </section>
        `;
    }

    getScript() {
        return ['/script/account/chart.js', '/script/account/transaction.js', '/script/dynamicLoading/boxloading.js'];
    }
}

export const banktransfer = ``;