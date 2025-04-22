<!DOCTYPE html>
<html>
<head>
    <title>Login</title>
</head>
<body>
    <h1>Login</h1>
    <#if error??>
        <p style="color:red">${error}</p>
    </#if>
    <form action="/login" method="post">
        <label>Username: <input type="text" name="username"/></label><br/>
        <label>Password: <input type="password" name="password"/></label><br/>
        <button type="submit">Log In</button>
    </form>
</body>
</html>