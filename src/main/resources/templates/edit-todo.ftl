<!DOCTYPE html>
<html>
<head>
    <title>${title}</title>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="/style.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">
</head>
<body>
    <header class="header">
        <div class="header-content">
            <h1>${title}</h1>
            <div class="auth-buttons">
                <a href="/" class="back-btn"><i class="fas fa-arrow-left"></i> Back to Tasks</a>
            </div>
        </div>
    </header>
    
    <main>
        <div class="message">
            <p>Edit your task details</p>
        </div>
        
        <div class="edit-form-container">
            <form action="/todos/${todo.id}" method="POST" class="edit-todo-form">
                <input type="hidden" name="returnFilter" value="${returnFilter}">
                
                <div class="form-group">
                    <label for="text">Task Title:</label>
                    <input type="text" id="text" name="text" value="${todo.text}" required>
                </div>
                
                <div class="form-group">
                    <label for="description">Description:</label>
                    <textarea id="description" name="description" rows="3" required>${todo.description}</textarea>
                </div>
                
                <div class="form-group">
                    <label for="date">Due Date:</label>
                    <input type="date" id="date" name="date" value="${todo.date}" required>
                </div>
                
                <div class="form-group checkbox-group">
                    <label class="checkbox-label">
                        <input type="checkbox" name="completed" ${(todo.completed)?then('checked', '')}>
                        Mark as completed
                    </label>
                </div>
                
                <div class="form-actions">
                    <button type="submit" class="save-btn">Save Changes</button>
                    <a href="/" class="cancel-btn">Cancel</a>
                </div>
            </form>
        </div>
    </main>
    
    <footer class="footer">
        <p>© ${.now?string('yyyy')} Jolt Framework - Powered by Freemarker</p>
    </footer>
</body>
</html>