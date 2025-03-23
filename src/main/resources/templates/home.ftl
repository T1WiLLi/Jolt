<!DOCTYPE html>
<html>
<head>
    <title>${title}</title>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="style.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">
</head>
<body>
    <header class="header">
        <div class="header-content">
            <h1>${title}</h1>
            <form method="GET" action="/" class="search-form">
                <input type="text" name="search" placeholder="Search todos..." value="${searchQuery!}" class="search-input">
                <button type="submit" class="search-btn"><i class="fas fa-search"></i></button>
            </form>
            <div class="auth-buttons">
                <form action="/auth/logout" method="POST">
                    <button type="submit" class="logout-btn">Logout</button>
                </form>
            </div>
        </div>
    </header>
    
    <main>
        <div class="message ${(completedCount == totalCount && totalCount > 0)?then('message-success', '')}">
            <p>${message}</p>
        </div>
        
        <#if error??>
            <div class="error-message">
                <p>${error}</p>
            </div>
        </#if>
        
        <div class="todo-container">
            <div class="toolbar">
                <div class="filter-options">
                    <a href="/?status=all" class="filter-btn ${(filterStatus == 'all')?then('active', '')}">All (${totalCount})</a>
                    <a href="/?status=pending" class="filter-btn ${(filterStatus == 'pending')?then('active', '')}">Pending (${pendingCount})</a>
                    <a href="/?status=completed" class="filter-btn ${(filterStatus == 'completed')?then('active', '')}">Completed (${completedCount})</a>
                    <a href="/?status=today" class="filter-btn ${(filterStatus == 'today')?then('active', '')}">Today</a>
                    <select name="priority" onchange="window.location.href='/?priority=' + this.value" class="filter-btn">
                        <option value="">Priority</option>
                        <option value="Low" ${(priority == "Low")?then('selected', '')}>Low</option>
                        <option value="Medium" ${(priority == "Medium")?then('selected', '')}>Medium</option>
                        <option value="High" ${(priority == "High")?then('selected', '')}>High</option>
                    </select>
                    <select name="category" onchange="window.location.href='/?category=' + this.value" class="filter-btn">
                        <option value="">Category</option>
                        <option value="Work" ${(category == "Work")?then('selected', '')}>Work</option>
                        <option value="Personal" ${(category == "Personal")?then('selected', '')}>Personal</option>
                        <option value="Shopping" ${(category == "Shopping")?then('selected', '')}>Shopping</option>
                        <option value="Other" ${(category == "Other")?then('selected', '')}>Other</option>
                    </select>
                </div>
                
                <div class="sort-options">
                    <form method="GET" action="/" class="sort-form">
                        <input type="hidden" name="status" value="${filterStatus}">
                        <input type="hidden" name="search" value="${searchQuery!}">
                        <input type="hidden" name="priority" value="${priority}">
                        <input type="hidden" name="category" value="${category}">
                        <select name="sort" onchange="this.form.submit()" class="sort-select">
                            <option value="date-asc" ${(sortBy == 'date-asc')?then('selected', '')}>Date (Oldest)</option>
                            <option value="date-desc" ${(sortBy == 'date-desc')?then('selected', '')}>Date (Newest)</option>
                            <option value="alpha" ${(sortBy == 'alpha')?then('selected', '')}>Alphabetical</option>
                            <option value="completed" ${(sortBy == 'completed')?then('selected', '')}>Completion Status</option>
                        </select>
                    </form>
                </div>
            </div>
            
            <form action="/todos" method="POST" class="add-todo-form">
                <div class="add-todo-inputs">
                    <input type="text" name="text" placeholder="Add a new task..." class="new-todo-input" required>
                    <input type="text" name="description" placeholder="Description" class="new-todo-input" required>
                    <input type="date" name="date" class="new-todo-input" value="${today}" required>
                    <select name="priority" class="new-todo-input" required>
                        <option value="Low">Low</option>
                        <option value="Medium">Medium</option>
                        <option value="High">High</option>
                    </select>
                    <select name="category" class="new-todo-input" required>
                        <option value="Work">Work</option>
                        <option value="Personal">Personal</option>
                        <option value="Shopping">Shopping</option>
                        <option value="Other">Other</option>
                    </select>
                </div>
                <button type="submit" class="add-todo-btn">Add Task</button>
            </form>
            
            <div class="todo-list">
                <#if todos?? && todos?size gt 0>
                    <#list todos as todo>
                        <div class="todo-item <#if todo.completed>completed</#if> <#if todo.date == today>today</#if>" data-id="${todo.id}">
                            <div class="todo-main">
                                <form action="/todos/toggle/${todo.id}" method="POST" class="toggle-form">
                                    <input type="hidden" name="returnFilter" value="${filterStatus}">
                                    <button type="submit" class="checkbox-btn">
                                        <i class="<#if todo.completed>fas fa-check-circle<#else>far fa-circle</#if>"></i>
                                    </button>
                                </form>
                                <div class="todo-content">
                                    <div class="todo-primary">
                                        <span class="todo-text">${todo.text}</span>
                                        <#if todo.date == today>
                                            <span class="today-badge">Today</span>
                                        </#if>
                                        <span class="priority-badge ${todo.priority?lower_case}">${todo.priority}</span>
                                        <span class="category-badge">${todo.category}</span>
                                    </div>
                                    <div class="todo-secondary">
                                        <span class="todo-description">${todo.description}</span>
                                        <span class="todo-date"><i class="far fa-calendar-alt"></i> ${todo.date}</span>
                                    </div>
                                </div>
                            </div>
                            <div class="todo-actions">
                                <a href="/todos/edit/${todo.id}?returnFilter=${filterStatus}" class="edit-btn">
                                    <i class="fas fa-edit"></i>
                                </a>
                                <a href="/todos/delete/${todo.id}?returnFilter=${filterStatus}" class="delete-btn" 
                                   onclick="return confirm('Are you sure you want to delete this task?')">
                                    <i class="fas fa-trash-alt"></i>
                                </a>
                            </div>
                        </div>
                    </#list>
                    
                    <div class="todo-actions-footer">
                        <div class="stats">
                            Completed: ${completedCount} / ${totalCount}
                        </div>
                        <#if completedCount gt 0>
                            <form action="/todos/clear-completed" method="POST">
                                <button type="submit" class="clear-completed-btn" 
                                        onclick="return confirm('Are you sure you want to clear all completed tasks?')">
                                    Clear Completed
                                </button>
                            </form>
                        </#if>
                    </div>
                <#else>
                    <div class="no-todos">
                        <i class="fas fa-clipboard-check empty-icon"></i>
                        <p>No tasks found<#if searchQuery?has_content> matching "${searchQuery}"</#if></p>
                        <#if searchQuery?has_content>
                            <a href="/" class="clear-search-btn">Clear search</a>
                        </#if>
                    </div>
                </#if>
            </div>
        </div>
    </main>
    
    <footer class="footer">
        <p>© ${.now?string('yyyy')} Jolt Framework - Powered by Freemarker</p>
    </footer>
</body>
</html>