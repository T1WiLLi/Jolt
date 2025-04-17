<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>XSS Test</title>
</head>
<body>
    <h1>XSS Test</h1>
    <p>Rendered input: ${input}</p>
    <p>Check the browser console for alerts or inspect the page source.</p>
    <p id="test">This will become GREEN if JS is allowed</p>
    <script nonce="${nonce()}">
        document.getElementById('test').style.color = 'green';
    </script>
</body>
</html>