$('[data-current]').text('$' + loggedInUser.Account.money['main-account']);
$('[data-saving]').text('$' + loggedInUser.Account.money['saving-account']);
$('[data-placement]').text('$' + loggedInUser.Account.money['placement-account']);