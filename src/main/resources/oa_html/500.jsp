<%-- 500.jsp — Internal Server Error --%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <title>${appName} — Kesalahan Server</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;700;800&display=swap" rel="stylesheet">
    <style>
        *{margin:0;padding:0;box-sizing:border-box;}
        body{font-family:'Inter',sans-serif;background:#0a0a1a;color:#e0e0ee;min-height:100vh;display:flex;justify-content:center;align-items:center;text-align:center;}
        .wrap{padding:40px;}
        .code{font-size:80px;font-weight:800;color:#e74c3c;}
        h2{font-size:20px;margin:16px 0 8px;font-weight:700;}
        p{color:#6a6a8a;font-size:14px;margin-bottom:24px;}
        .err{background:rgba(231,76,60,0.08);border:1px solid rgba(231,76,60,0.2);border-radius:8px;padding:12px 20px;font-size:13px;color:#e74c3c;margin-bottom:24px;}
        a{color:#c9a227;text-decoration:none;font-weight:600;font-size:14px;padding:10px 24px;border:1px solid rgba(201,162,39,0.3);border-radius:8px;transition:all .2s;}
        a:hover{background:rgba(201,162,39,0.1);}
    </style>
</head>
<body>
    <div class="wrap">
        <div class="code">500</div>
        <h2>Kesalahan Internal Server</h2>
        <div class="err">${errorMessage}</div>
        <a href="/OA_HTML/AppsHome.jsp">Kembali ke Home</a>
    </div>
</body>
</html>
