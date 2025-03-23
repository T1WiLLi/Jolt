document.addEventListener('DOMContentLoaded', function () {
    setupTodoCheckboxes();
    setupAddTodoForm();
    setupEditButtons();
    setupDeleteButtons();
    setupModalEvents();
    setupLogoutButton();
});

function setupTodoCheckboxes() {
    const checkboxes = document.querySelectorAll('.todo-checkbox');
    checkboxes.forEach(checkbox => {
        checkbox.addEventListener('change', function () {
            const id = this.getAttribute('data-id');
            const completed = this.checked;

            const todoItem = document.querySelector(`.todo-item[data-id='${id}']`);
            if (completed) {
                todoItem.classList.add('completed');
            } else {
                todoItem.classList.remove('completed');
            }

            updateStats();
            updateTodo(id, { completed: completed });
        });
    });
}

function setupAddTodoForm() {
    const addButton = document.getElementById('add-todo-btn');
    const newTodoInput = document.getElementById('new-todo-text');
    const newTodoDescription = document.getElementById('new-todo-description');
    const newTodoDate = document.getElementById('new-todo-date');

    addButton.addEventListener('click', function () {
        addNewTodo();
    });

    newTodoInput.addEventListener('keypress', function (e) {
        if (e.key === 'Enter') {
            addNewTodo();
        }
    });
}

function addNewTodo() {
    const input = document.getElementById('new-todo-text');
    const description = document.getElementById('new-todo-description');
    const date = document.getElementById('new-todo-date');
    const text = input.value.trim();
    const desc = description.value.trim();
    const todoDate = date.value;

    if (text && desc && todoDate) {
        fetch('/todos', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ text: text, completed: false, description: desc, date: todoDate })
        })
            .then(response => response.json())
            .then(todo => {
                addTodoToUI(todo);
                input.value = '';
                description.value = '';
                date.value = '';
                updateStats();
            })
            .catch(error => console.error('Error adding todo:', error));
    }
}

function addTodoToUI(todo) {
    const todoList = document.querySelector('.todo-list');
    const noTodosMessage = document.querySelector('.no-todos');

    if (noTodosMessage) {
        noTodosMessage.remove();
    }

    const todoItem = document.createElement('div');
    todoItem.className = `todo-item ${todo.completed ? 'completed' : ''}`;
    todoItem.setAttribute('data-id', todo.id);

    const todoActions = document.createElement('div');
    todoActions.className = 'todo-actions';

    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.className = 'todo-checkbox';
    checkbox.checked = todo.completed;
    checkbox.setAttribute('data-id', todo.id);

    checkbox.addEventListener('change', function () {
        const id = this.getAttribute('data-id');
        const completed = this.checked;

        if (completed) {
            todoItem.classList.add('completed');
        } else {
            todoItem.classList.remove('completed');
        }

        updateStats();
        updateTodo(id, { completed: completed });
    });

    const textSpan = document.createElement('span');
    textSpan.className = 'todo-text';
    textSpan.textContent = todo.text;

    const descriptionSpan = document.createElement('span');
    descriptionSpan.className = 'todo-description';
    descriptionSpan.textContent = todo.description;

    const dateSpan = document.createElement('span');
    dateSpan.className = 'todo-date';
    dateSpan.textContent = todo.date;

    todoActions.appendChild(checkbox);
    todoActions.appendChild(textSpan);
    todoActions.appendChild(descriptionSpan);
    todoActions.appendChild(dateSpan);

    const buttonsDiv = document.createElement('div');
    buttonsDiv.className = 'todo-item-buttons';

    const editBtn = document.createElement('button');
    editBtn.className = 'edit-btn';
    editBtn.textContent = 'Edit';
    editBtn.setAttribute('data-id', todo.id);

    editBtn.addEventListener('click', function () {
        const id = this.getAttribute('data-id');
        const todoText = this.closest('.todo-item').querySelector('.todo-text').textContent;
        const todoDescription = this.closest('.todo-item').querySelector('.todo-description').textContent;
        const todoDate = this.closest('.todo-item').querySelector('.todo-date').textContent;
        openEditModal(id, todoText, todoDescription, todoDate);
    });

    const deleteBtn = document.createElement('button');
    deleteBtn.className = 'delete-btn';
    deleteBtn.textContent = 'Delete';
    deleteBtn.setAttribute('data-id', todo.id);

    deleteBtn.addEventListener('click', function () {
        const id = this.getAttribute('data-id');
        deleteTodo(id);
    });

    buttonsDiv.appendChild(editBtn);
    buttonsDiv.appendChild(deleteBtn);

    todoItem.appendChild(todoActions);
    todoItem.appendChild(buttonsDiv);

    if (!document.querySelector('.stats')) {
        const statsDiv = document.createElement('div');
        statsDiv.className = 'stats';
        todoList.appendChild(statsDiv);
    }

    const statsDiv = document.querySelector('.stats');
    todoList.insertBefore(todoItem, statsDiv);
}

function setupEditButtons() {
    const editButtons = document.querySelectorAll('.edit-btn');
    editButtons.forEach(button => {
        button.addEventListener('click', function () {
            const id = this.getAttribute('data-id');
            const todoText = this.closest('.todo-item').querySelector('.todo-text').textContent;
            const todoDescription = this.closest('.todo-item').querySelector('.todo-description').textContent;
            const todoDate = this.closest('.todo-item').querySelector('.todo-date').textContent;
            openEditModal(id, todoText, todoDescription, todoDate);
        });
    });
}

function setupDeleteButtons() {
    const deleteButtons = document.querySelectorAll('.delete-btn');
    deleteButtons.forEach(button => {
        button.addEventListener('click', function () {
            const id = this.getAttribute('data-id');
            deleteTodo(id);
        });
    });
}

function setupModalEvents() {
    const modal = document.getElementById('edit-modal');
    const closeModalBtn = document.querySelector('.close-modal');
    const saveEditBtn = document.getElementById('save-edit-btn');
    const cancelEditBtn = document.getElementById('cancel-edit-btn');

    closeModalBtn.addEventListener('click', closeEditModal);
    cancelEditBtn.addEventListener('click', closeEditModal);

    saveEditBtn.addEventListener('click', function () {
        const todoId = modal.getAttribute('data-todo-id');
        const newText = document.getElementById('edit-todo-text').value.trim();
        const newDescription = document.getElementById('edit-todo-description').value.trim();
        const newDate = document.getElementById('edit-todo-date').value;

        if (newText && newDescription && newDate) {
            updateTodo(todoId, { text: newText, description: newDescription, date: newDate });
            document.querySelector(`.todo-item[data-id='${todoId}'] .todo-text`).textContent = newText;
            document.querySelector(`.todo-item[data-id='${todoId}'] .todo-description`).textContent = newDescription;
            document.querySelector(`.todo-item[data-id='${todoId}'] .todo-date`).textContent = newDate;
            closeEditModal();
        }
    });

    window.addEventListener('click', function (event) {
        if (event.target === modal) {
            closeEditModal();
        }
    });
}

function openEditModal(todoId, todoText, todoDescription, todoDate) {
    const modal = document.getElementById('edit-modal');
    const editInput = document.getElementById('edit-todo-text');
    const editDescription = document.getElementById('edit-todo-description');
    const editDate = document.getElementById('edit-todo-date');

    modal.setAttribute('data-todo-id', todoId);
    editInput.value = todoText;
    editDescription.value = todoDescription;
    editDate.value = todoDate;

    modal.style.display = 'block';
    editInput.focus();
}

function closeEditModal() {
    const modal = document.getElementById('edit-modal');
    modal.style.display = 'none';
}

function updateTodo(id, updates) {
    fetch(`/todos/${id}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(updates)
    })
        .then(response => response.json())
        .catch(error => console.error('Error updating todo:', error));
}

function deleteTodo(id) {
    if (confirm('Are you sure you want to delete this todo?')) {
        fetch(`/todos/${id}`, {
            method: 'DELETE'
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    const todoItem = document.querySelector(`.todo-item[data-id='${id}']`);
                    todoItem.remove();

                    updateStats();

                    const todoItems = document.querySelectorAll('.todo-item');
                    if (todoItems.length === 0) {
                        const todoList = document.querySelector('.todo-list');
                        const statsDiv = document.querySelector('.stats');

                        if (statsDiv) {
                            statsDiv.remove();
                        }

                        const noTodosMsg = document.createElement('p');
                        noTodosMsg.className = 'no-todos';
                        noTodosMsg.textContent = 'No todos yet! Enjoy your free time! 😊';
                        todoList.appendChild(noTodosMsg);
                    }
                }
            })
            .catch(error => console.error('Error deleting todo:', error));
    }
}

function updateStats() {
    const items = document.querySelectorAll('.todo-item');
    const completed = document.querySelectorAll('.todo-item.completed');
    const stats = document.querySelector('.stats');

    if (stats) {
        stats.textContent = `Completed: ${completed.length} / ${items.length}`;
    }
}

function setupLogoutButton() {
    const logoutBtn = document.getElementById('logout-btn');
    logoutBtn.addEventListener('click', function () {
        fetch('/auth/logout', {
            method: 'POST'
        })
            .then(response => {
                if (response.redirected) {
                    window.location.href = response.url;
                } else {
                    return response.json();
                }
            })
            .then(data => {
                if (data && data.message) {
                    console.log(data.message);
                }
            })
            .catch(error => console.error('Error logging out:', error));
    });
}