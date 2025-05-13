<#assign dateFormat = date?string("EEEE, MMMM d, yyyy")>
<!DOCTYPE html>
<html>
<head>
    <title>${title}</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            background-color: #f4f4f4;
            color: #333;
            margin: 0;
            padding: 20px;
        }
        .container {
            max-width: 800px;
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0px 4px 8px rgba(0,0,0,0.2);
        }
        h1 {
            color: #007bff;
        }
        ul {
            padding: 0;
        }
        li {
            list-style: none;
            background: #007bff;
            color: white;
            padding: 8px;
            margin: 5px 0;
            border-radius: 5px;
        }
        .info-section {
            margin-top: 20px;
            padding: 15px;
            background: #e3e3e3;
            border-radius: 5px;
        }
        footer {
            margin-top: 20px;
            font-size: 0.9em;
            color: #777;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>${title}</h1>
        <p>${message}</p>
        <p><strong>User:</strong> ${user.name}</p>

        <h2>Items List:</h2>
        <ul>
            <#list items as item>
                <li>${item}</li>
            </#list>
        </ul>

        <#if showSection>
            <div class="info-section">
                <p><strong>Date:</strong> ${dateFormat}</p>
                <p><strong>Formatted Number:</strong> ${number?string("0.00")}</p>
            </div>
        </#if>

        <footer>
            <p>${copyright}</p>
        </footer>
    </div>
</body>
</html>
