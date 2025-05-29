<#-- index.ftl: Test page for Flash component -->
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Flash Message Test Stand</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #f0f4f8; }
    .wrapper {
      padding: 2rem;
      max-width: 800px;
      margin: 5rem auto 2rem;
      background: white;
      border-radius: 1rem;
      box-shadow: 0 8px 24px rgba(0,0,0,0.05);
    }
    h1 { text-align: center; color: #2d3748; margin-bottom: 0.5rem; font-size: 2.5rem; }
    .subtitle { text-align: center; color: #4a5568; margin-bottom: 2rem; font-size: 1.1rem; }
    .button-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 1rem;
      margin-bottom: 2rem;
    }
    .flash-form button {
      width: 100%; padding: 1rem;
      font-size: 1rem; font-weight: 600;
      border: none; border-radius: 0.5rem;
      cursor: pointer; transition: transform 0.2s;
    }
    .flash-form button:hover { transform: translateY(-2px); }
    .success-btn { background: #48bb78; color: white; }
    .error-btn   { background: #f56565; color: white; }
    .warning-btn { background: #ed8936; color: white; }
    .info-btn    { background: #4299e1; color: white; }
  </style>
</head>
<body>
  <#include "components/Flash.ftl">

  <div class="wrapper">
    <h1>⚡ Flash Message Test Stand</h1>
    <p class="subtitle">Trigger different flash types below</p>

    <div class="button-grid">
      <form method="post" action="/flash-test/success" class="flash-form">
        <button type="submit" class="success-btn">✅ Success</button>
      </form>
      <form method="post" action="/flash-test/error" class="flash-form">
        <button type="submit" class="error-btn">❌ Error</button>
      </form>
      <form method="post" action="/flash-test/warning" class="flash-form">
        <button type="submit" class="warning-btn">⚠️ Warning</button>
      </form>
      <form method="post" action="/flash-test/info" class="flash-form">
        <button type="submit" class="info-btn">ℹ️ Info</button>
      </form>
    </div>

    <div style="text-align:center;">
      <form method="post" action="/flash-test/clear" style="display:inline-block;">
        <button type="submit" style="padding:0.75rem 1.5rem; font-size:1rem; border:none; border-radius:0.5rem; background:#a0aec0; color:white; cursor:pointer;">
          🗑️ Clear Flash
        </button>
      </form>
    </div>
  </div>
</body>
</html>