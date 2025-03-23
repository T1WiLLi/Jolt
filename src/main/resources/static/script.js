document.addEventListener('DOMContentLoaded', function () {
    setupTodoCheckboxes();
    setupAddTodoForm();
    setupEditButtons();
    setupDeleteButtons();
    setupModalEvents();
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
    const text = input.value.trim();

    if (text) {
        fetch('/todos', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ text: text, completed: false })
        })
            .then(response => response.json())
            .then(todo => {
                addTodoToUI(todo);
                input.value = '';
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

    todoActions.appendChild(checkbox);
    todoActions.appendChild(textSpan);

    const buttonsDiv = document.createElement('div');
    buttonsDiv.className = 'todo-item-buttons';

    const editBtn = document.createElement('button');
    editBtn.className = 'edit-btn';
    editBtn.textContent = 'Edit';
    editBtn.setAttribute('data-id', todo.id);

    editBtn.addEventListener('click', function () {
        const id = this.getAttribute('data-id');
        openEditModal(id, todo.text);
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
            openEditModal(id, todoText);
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

        if (newText) {
            updateTodo(todoId, { text: newText });
            document.querySelector(`.todo-item[data-id='${todoId}'] .todo-text`).textContent = newText;
            closeEditModal();
        }
    });

    window.addEventListener('click', function (event) {
        if (event.target === modal) {
            closeEditModal();
        }
    });
}

function openEditModal(todoId, todoText) {
    const modal = document.getElementById('edit-modal');
    const editInput = document.getElementById('edit-todo-text');

    modal.setAttribute('data-todo-id', todoId);
    editInput.value = todoText;

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