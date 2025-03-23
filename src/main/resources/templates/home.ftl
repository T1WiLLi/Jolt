<!DOCTYPE html>
<html>
<head>
    <title>${title}</title>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="style.css">
</head>
<body>
   <header class="header">
        <h1>${title}</h1>
        <div class="auth-buttons">
            <button id="logout-btn" class="logout-btn">Logout</button>
        </div>
    </header>
    
    <main>
        <div class="message">
            <p>${message}</p>
        </div>
        
        <div class="todo-container">
            <h2>My Todo List:</h2>
            
            <div class="add-todo-form">
                <input type="text" id="new-todo-text" placeholder="Add a new task..." class="new-todo-input">
                <input type="text" id="new-todo-description" placeholder="Description" class="new-todo-input">
                <input type="date" id="new-todo-date" class="new-todo-input">
                <button id="add-todo-btn" class="add-todo-btn">Add</button>
            </div>
            
            <div class="todo-list">
                <#if todos?? && todos?size gt 0>
                    <#list todos as todo>
                        <div class="todo-item <#if todo.completed>completed</#if>" data-id="${todo.id}">
                            <div class="todo-actions">
                                <input type="checkbox" 
                                       class="todo-checkbox" 
                                       <#if todo.completed>checked</#if>
                                       data-id="${todo.id}">
                                <span class="todo-text">${todo.text}</span>
                                <span class="todo-description">${todo.description}</span>
                                <span class="todo-date">${todo.date}</span>
                            </div>
                            <div class="todo-item-buttons">
                                <button class="edit-btn" data-id="${todo.id}">Edit</button>
                                <button class="delete-btn" data-id="${todo.id}">Delete</button>
                            </div>
                        </div>
                    </#list>
                    
                    <div class="stats">
                        Completed: ${todos?filter(t -> t.completed)?size} / ${todos?size}
                    </div>
                <#else>
                    <p class="no-todos">No todos yet! Enjoy your free time! 😊</p>
                </#if>
            </div>
        </div>
        
        <div id="edit-modal" class="modal">
            <div class="modal-content">
                <span class="close-modal">&times;</span>
                <h3>Edit Todo</h3>
                <input type="text" id="edit-todo-text" class="edit-todo-input">
                <input type="text" id="edit-todo-description" class="edit-todo-input">
                <input type="date" id="edit-todo-date" class="edit-todo-input">
                <div class="modal-buttons">
                    <button id="save-edit-btn" class="save-btn">Save</button>
                    <button id="cancel-edit-btn" class="cancel-btn">Cancel</button>
                </div>
            </div>
        </div>
    </main>
    
    <footer class="footer">
        <p>© ${.now?string('yyyy')} Jolt Framework - Powered by Freemarker</p>
    </footer>
    <script src="script.js"></script>
</body>
</html>