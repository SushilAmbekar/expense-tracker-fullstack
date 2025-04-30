let expenses = [];
let total = 0;

function addExpense() {
  const description = document.getElementById('description').value;
  const amount = parseFloat(document.getElementById('amount').value);

  if (description && !isNaN(amount)) {
    expenses.push({ description, amount });
    total += amount;
    updateUI();
    document.getElementById('description').value = '';
    document.getElementById('amount').value = '';
  }
}

function updateUI() {
  const list = document.getElementById('expense-list');
  const totalDisplay = document.getElementById('total');
  list.innerHTML = '';
  expenses.forEach((expense) => {
    const li = document.createElement('li');
    li.textContent = `${expense.description} - ₹${expense.amount}`;
    li.classList.add('expense-item');
    list.appendChild(li);
  });
  totalDisplay.textContent = total.toFixed(2);
}

window.onload = function () {
  updateUI();
};
