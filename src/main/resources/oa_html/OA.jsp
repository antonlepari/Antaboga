<%-- OA.jsp — Dynamic Page Renderer --%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${appName} — ${pageTitle}</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap" rel="stylesheet">
    <style>
        *{margin:0;padding:0;box-sizing:border-box;}
        body{font-family:'Inter',sans-serif;background:#0a0a1a;color:#e0e0ee;min-height:100vh;}
        .bar{background:#111128;border-bottom:1px solid rgba(201,162,39,0.12);padding:0 32px;height:50px;display:flex;align-items:center;justify-content:space-between;}
        .bar a{color:#6a6a8a;text-decoration:none;font-size:13px;}.bar a:hover{color:#c9a227;}
        .bar .title{font-weight:700;color:#c9a227;font-size:14px;}
        .content{max-width:1000px;margin:40px auto;padding:0 32px;}
        .content h2{font-size:22px;font-weight:700;margin-bottom:20px;}
    </style>
</head>
<body>
    <div class="bar">
        <a href="/OA_HTML/AppsHome.jsp">← Kembali ke Home</a>
        <span class="title">${pageTitle}</span>
        <a href="/OA_HTML/AppsLogout.jsp">Logout</a>
    </div>
    <div class="content">
        <h2>${pageTitle}</h2>
        ${pageContent}
    </div>
</body>
</html>
