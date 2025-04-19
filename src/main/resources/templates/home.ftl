<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Home</title>
</head>
<body>
  <h1>Shopping List</h1>

  <ul>
  <#-- “items” is the List<String> we passed in -->
  <#list items as item>
    <li>${item}</li>
  </#list>
  </ul>

  <h2>Add a new item</h2>
  <form method="post" action="/add">
    <input type="text" name="item" placeholder="Enter item…" required/>
    <button type="submit">Add</button>
  </form>
</body>
</html>
