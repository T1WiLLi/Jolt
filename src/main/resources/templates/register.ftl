<!DOCTYPE html>
<html>
<head>
    <title>${title}</title>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="/style.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">
</head>
<body class="auth-page">
    <div class="auth-container">
        <div class="auth-header">
            <i class="fas fa-tasks app-icon"></i>
            <h1>Todo Manager</h1>
        </div>

        <div class="auth-card">
            <h2>${title}</h2>
            <p class="auth-subtitle">${message}</p>

            <#if error?? && error?has_content>
                <div class="error-message">
                    <i class="fas fa-exclamation-circle"></i>
                    <p>${error}</p>
                </div>
            </#if>

            <form action="/auth/register" method="POST" class="auth-form">
                <div class="form-group">
                    <label for="username">
                        <i class="fas fa-user"></i>
                        Username
                    </label>
                    <input type="text" id="username" name="username" placeholder="Choose a username" required>
                </div>
                
                <div class="form-group">
                    <label for="password">
                        <i class="fas fa-lock"></i>
                        Password
                    </label>
                    <input type="password" id="password" name="password" placeholder="Create a password" required>
                </div>
                
                <button type="submit" class="auth-btn">
                    <i class="fas fa-user-plus"></i> Register
                </button>
            </form>
            
            <div class="auth-links">
                <p>Already have an account? <a href="/auth/login">Login here</a></p>
            </div>
        </div>
        
        <div class="app-footer">
            <p>© ${.now?string('yyyy')} Jolt Framework</p>
        </div>
    </div>
</body>
</html>