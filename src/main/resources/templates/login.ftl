<!DOCTYPE html>
<html>
<head>
    <title>${title}</title>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="/style.css">
</head>
<body>
    <header class="header">
        <h1>${title}</h1>
    </header>

    <main>
        <div class="message">
            <p>${message}</p>
        </div>

        <#if error??>
            <div class="error-message">
                <p>${error}</p>
            </div>
        </#if>

        <div class="form-container">
            <form action="/auth/login" method="POST">
                <div class="form-group">
                    <label for="username">Username:</label>
                    <input type="text" id="username" name="username" required>
                </div>
                <div class="form-group">
                    <label for="password">Password:</label>
                    <input type="password" id="password" name="password" required>
                </div>
                <button type="submit" class="submit-btn">Login</button>
            </form>
        </div>

        <div class="auth-links">
            <p>Don't have an account? <a href="/auth/register">Register here</a></p>
        </div>
    </main>

    <footer class="footer">
        <p>© ${.now?string('yyyy')} Jolt Framework - Powered by Freemarker</p>
    </footer>
</body>
</html>