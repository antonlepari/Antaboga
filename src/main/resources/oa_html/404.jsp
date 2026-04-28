<%-- 404.jsp — Page Not Found --%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <title>${appName} — Halaman Tidak Ditemukan</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;700;800&display=swap" rel="stylesheet">
    <style>
        *{margin:0;padding:0;box-sizing:border-box;}
        body{font-family:'Inter',sans-serif;background:#0a0a1a;color:#e0e0ee;min-height:100vh;display:flex;justify-content:center;align-items:center;text-align:center;}
        .wrap{padding:40px;}
        .code{font-size:80px;font-weight:800;background:linear-gradient(135deg,#c9a227,#f7d06b);-webkit-background-clip:text;-webkit-text-fill-color:transparent;background-clip:text;}
        h2{font-size:20px;margin:16px 0 8px;font-weight:700;}
        p{color:#6a6a8a;font-size:14px;margin-bottom:24px;}
        a{color:#c9a227;text-decoration:none;font-weight:600;font-size:14px;padding:10px 24px;border:1px solid rgba(201,162,39,0.3);border-radius:8px;transition:all .2s;}
        a:hover{background:rgba(201,162,39,0.1);}
    </style>
</head>
<body>
    <div class="wrap">
        <div class="code">404</div>
        <h2>Halaman Tidak Ditemukan</h2>
        <p>Halaman yang Anda cari tidak tersedia di server ini.</p>
        <a href="/OA_HTML/AppsHome.jsp">Kembali ke Home</a>
    </div>
</body>
</html>
