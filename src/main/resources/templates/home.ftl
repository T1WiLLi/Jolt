<!DOCTYPE html>
<html>
<head>
    <title>${title}</title>
    <link rel="stylesheet" href="./style.css">
</head>
<body>
    <header>
        <h1>${title}</h1>
    </header>
    
    <main>
        <div class="message">
            <p>${message}</p>
        </div>
        
        <div class="items">
            <h2>Items:</h2>
            <ul>
                <#list items as item>
                    <li>${item}</li>
                </#list>
            </ul>
        </div>
    </main>
    
    <footer>
        <p>&copy; ${.now?string('yyyy')} Jolt Framework</p>
    </footer>
    
    <script src="/js/main.js"></script>
</body>
</html>