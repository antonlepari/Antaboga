<%-- runforms.jsp — Java Web Start Forms Launcher --%>
<%-- Mimics Oracle EBS: /OA_HTML/runforms.jsp?resp_app=...&start_func=... --%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="robots" content="noindex, nofollow">
    <title>${appName} — Launching ${funcTitle}</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        :root{--bg:#0a0a1a;--bg2:#111128;--card:rgba(22,22,58,0.7);--border:rgba(201,162,39,0.12);--gold:#c9a227;--gold-l:#f7d06b;--text:#e0e0ee;--dim:#6a6a8a;--muted:#444466;--success:#2ecc71;}
        *{margin:0;padding:0;box-sizing:border-box;}
        body{font-family:'Inter',sans-serif;background:var(--bg);color:var(--text);min-height:100vh;display:flex;flex-direction:column;align-items:center;justify-content:center;-webkit-font-smoothing:antialiased;}

        .launcher{text-align:center;max-width:520px;padding:40px;}
        .launcher-icon{font-size:64px;margin-bottom:24px;animation:pulse 2s ease-in-out infinite;}
        @keyframes pulse{0%,100%{transform:scale(1);}50%{transform:scale(1.08);}}

        .launcher h1{font-size:22px;font-weight:800;margin-bottom:8px;}
        .launcher h1 span{background:linear-gradient(135deg,var(--gold),var(--gold-l));-webkit-background-clip:text;-webkit-text-fill-color:transparent;background-clip:text;}
        .launcher p{color:var(--dim);font-size:14px;line-height:1.6;margin-bottom:28px;}

        .params-box{background:var(--card);border:1px solid var(--border);border-radius:14px;padding:20px 24px;text-align:left;margin-bottom:28px;font-size:12px;font-family:'Menlo','Consolas',monospace;}
        .params-box .row{display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid rgba(255,255,255,0.03);}
        .params-box .row:last-child{border:none;}
        .params-box .key{color:var(--dim);font-weight:600;}
        .params-box .val{color:var(--gold-l);text-align:right;max-width:200px;overflow:hidden;text-overflow:ellipsis;}

        .status{margin-bottom:24px;}
        .status-msg{font-size:14px;font-weight:600;display:flex;align-items:center;justify-content:center;gap:8px;}
        .spinner{width:20px;height:20px;border:2px solid var(--border);border-top-color:var(--gold);border-radius:50%;animation:spin 0.8s linear infinite;display:inline-block;}
        @keyframes spin{to{transform:rotate(360deg);}}
        .status-ok{color:var(--success);}
        .status-waiting{color:var(--gold-l);}

        .actions{display:flex;gap:12px;justify-content:center;flex-wrap:wrap;}
        .btn{padding:12px 28px;border-radius:10px;font-size:14px;font-weight:700;font-family:inherit;cursor:pointer;transition:all .2s;text-decoration:none;display:inline-flex;align-items:center;gap:8px;border:none;}
        .btn-primary{background:linear-gradient(135deg,var(--gold),#b08a20);color:#0a0a14;}
        .btn-primary:hover{transform:translateY(-2px);box-shadow:0 4px 16px rgba(201,162,39,0.35);}
        .btn-secondary{background:rgba(255,255,255,0.04);border:1px solid var(--border);color:var(--dim);}
        .btn-secondary:hover{color:var(--text);background:rgba(255,255,255,0.06);}

        .footer{position:fixed;bottom:20px;color:var(--muted);font-size:11px;}
    </style>
</head>
<body>
    <div class="launcher">
        <div class="launcher-icon">🐉</div>
        <h1>Membuka <span>${funcTitle}</span></h1>
        <p>Antaboga ERP sedang meminta sistem untuk membuka modul ini menggunakan <strong>Java Web Start (JavawsLauncher)</strong>.</p>

        <div class="params-box">
            <div class="row"><span class="key">resp_app</span><span class="val">${respApp}</span></div>
            <div class="row"><span class="key">resp_key</span><span class="val">${respKey}</span></div>
            <div class="row"><span class="key">secgrp_key</span><span class="val">${secgrpKey}</span></div>
            <div class="row"><span class="key">start_func</span><span class="val">${startFunc}</span></div>
            <div class="row"><span class="key">server</span><span class="val">${serverHost}</span></div>
        </div>

        <div class="status">
            <div class="status-msg status-waiting" id="statusMsg">
                <span class="spinner"></span>
                Menunggu JavawsLauncher...
            </div>
        </div>

        <div class="actions">
            <button class="btn btn-primary" onclick="launchJnlp()">🔄 Coba Lagi</button>
            <a href="/OA_HTML/RF.jsp?function_id=${startFunc}" class="btn btn-secondary">← Kembali</a>
        </div>
    </div>

    <div class="footer">🐉 ${appName} v${appVersion} &middot; ${serverInfo}</div>

    <script>
        function launchJnlp() {
            var statusEl = document.getElementById('statusMsg');
            statusEl.className = 'status-msg status-waiting';
            statusEl.innerHTML = '<span class="spinner"></span> Menunggu JavawsLauncher...';

            // Build JNLP protocol URL — triggers "Open JavawsLauncher?" popup
            var host = window.location.host;
            var func = '${startFunc}';
            var protoUrl = 'jnlp://' + host + '/launch/antaboga.jnlp?func=' + encodeURIComponent(func);

            // Method 1: iframe protocol trigger (works on most browsers)
            var iframe = document.createElement('iframe');
            iframe.style.display = 'none';
            iframe.src = protoUrl;
            document.body.appendChild(iframe);

            // Update status after delay
            setTimeout(function() {
                try { document.body.removeChild(iframe); } catch(e){}
                statusEl.className = 'status-msg status-ok';
                statusEl.innerHTML = '✅ Permintaan terkirim. Jika popup tidak muncul, klik "Coba Lagi".';
            }, 3000);
        }

        // Auto-launch on page load
        window.addEventListener('load', function() {
            setTimeout(launchJnlp, 500);
        });
    </script>
</body>
</html>
