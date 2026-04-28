<%-- RF.jsp — Responsibility Function (Module Viewer) --%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="robots" content="noindex, nofollow">
    <title>${appName} — ${moduleTitle}</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        :root{--bg:#0a0a1a;--bg2:#111128;--card:rgba(22,22,58,0.7);--border:rgba(201,162,39,0.12);--gold:#c9a227;--gold-l:#f7d06b;--text:#e0e0ee;--dim:#6a6a8a;--muted:#444466;--success:#2ecc71;--warning:#f39c12;--danger:#e74c3c;}
        *{margin:0;padding:0;box-sizing:border-box;}
        body{font-family:'Inter',sans-serif;background:var(--bg);color:var(--text);min-height:100vh;-webkit-font-smoothing:antialiased;}

        .topbar{background:var(--bg2);border-bottom:1px solid var(--border);padding:0 32px;height:56px;display:flex;align-items:center;justify-content:space-between;position:sticky;top:0;z-index:100;}
        .topbar-left{display:flex;align-items:center;gap:12px;}
        .topbar-logo{font-size:18px;font-weight:800;background:linear-gradient(135deg,var(--gold),var(--gold-l));-webkit-background-clip:text;-webkit-text-fill-color:transparent;background-clip:text;text-decoration:none;}
        .topbar-sep{width:1px;height:24px;background:var(--border);}
        .breadcrumb{display:flex;align-items:center;gap:8px;font-size:13px;color:var(--dim);}
        .breadcrumb a{color:var(--dim);text-decoration:none;transition:color .2s;}
        .breadcrumb a:hover{color:var(--gold);}
        .breadcrumb .current{color:var(--text);font-weight:600;}
        .topbar-right{display:flex;align-items:center;gap:12px;}
        .topbar-right a{color:var(--dim);text-decoration:none;font-size:13px;padding:6px 14px;border-radius:6px;transition:all .2s;}
        .topbar-right a:hover{color:var(--text);background:rgba(255,255,255,0.04);}
        .topbar-right .logout{color:var(--danger);border:1px solid rgba(231,76,60,0.2);font-weight:600;}
        .topbar-right .logout:hover{background:rgba(231,76,60,0.1);}

        .page{max-width:1200px;margin:0 auto;padding:32px;}
        .page-header{margin-bottom:28px;display:flex;justify-content:space-between;align-items:center;}
        .page-header h1{font-size:24px;font-weight:800;}
        .page-header p{color:var(--dim);font-size:13px;margin-top:4px;}

        /* Module Navigation Tabs */
        .mod-tabs{display:flex;gap:4px;margin-bottom:28px;padding:4px;background:var(--bg2);border-radius:12px;border:1px solid var(--border);overflow-x:auto;}
        .mod-tab{
            padding:10px 20px;border-radius:8px;color:var(--dim);text-decoration:none;
            font-size:13px;font-weight:500;transition:all .2s;white-space:nowrap;
        }
        .mod-tab:hover{color:var(--text);background:rgba(255,255,255,0.04);}
        .mod-tab.active{color:var(--gold);background:rgba(201,162,39,0.1);font-weight:600;}

        /* Data Table */
        .oaTable{width:100%;border-collapse:collapse;background:var(--card);border:1px solid var(--border);border-radius:14px;overflow:hidden;}
        .oaTable thead{background:rgba(0,0,0,0.3);}
        .oaTable th{padding:14px 20px;text-align:left;font-size:11px;font-weight:700;color:var(--muted);text-transform:uppercase;letter-spacing:1px;border-bottom:1px solid var(--border);}
        .oaTable td{padding:14px 20px;font-size:14px;border-bottom:1px solid rgba(255,255,255,0.02);}
        .oaTable tr:hover td{background:rgba(201,162,39,0.03);}
        .oaTable .badge{display:inline-block;padding:3px 10px;border-radius:20px;font-size:11px;font-weight:600;background:rgba(46,204,113,0.12);color:var(--success);}
        .oaTable .emptyRow{text-align:center;padding:40px;color:var(--muted);font-size:14px;}

        /* Summary Cards */
        .oaSummary{display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:16px;margin-bottom:24px;}
        .oaSumCard{background:var(--card);border:1px solid var(--border);border-radius:14px;padding:24px;text-align:center;}
        .oaSumLabel{font-size:12px;color:var(--dim);margin-bottom:8px;font-weight:600;text-transform:uppercase;letter-spacing:1px;}
        .oaSumValue{font-size:28px;font-weight:800;color:var(--gold-l);}
        .oaSumValue.success{color:var(--success);}
        .oaSumValue.warning{color:var(--warning);}

        .errorText{color:var(--danger);font-size:14px;}

        /* Table Header with Add Button */
        .tableHeader{display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;}
        .tableHeader span{font-size:16px;font-weight:700;}
        .btnAdd{padding:10px 20px;background:linear-gradient(135deg,var(--gold),#b08a20);color:#0a0a14;border-radius:8px;text-decoration:none;font-size:13px;font-weight:700;transition:all .2s;}
        .btnAdd:hover{transform:translateY(-2px);box-shadow:0 4px 16px rgba(201,162,39,0.35);}

        /* Action Buttons in Table */
        .actions{white-space:nowrap;}
        .btnEdit{padding:4px 12px;background:rgba(52,152,219,0.12);border:1px solid rgba(52,152,219,0.25);color:#3498db;border-radius:6px;text-decoration:none;font-size:12px;font-weight:600;transition:all .2s;}
        .btnEdit:hover{background:rgba(52,152,219,0.2);}
        .btnDel{padding:4px 12px;background:rgba(231,76,60,0.08);border:1px solid rgba(231,76,60,0.2);color:var(--danger);border-radius:6px;text-decoration:none;font-size:12px;font-weight:600;transition:all .2s;margin-left:6px;}
        .btnDel:hover{background:rgba(231,76,60,0.15);}

        /* Flash Messages */
        .flash{padding:14px 20px;border-radius:10px;font-size:14px;font-weight:500;margin-bottom:20px;animation:flashIn .4s ease;}
        .flash.success{background:rgba(46,204,113,0.1);border:1px solid rgba(46,204,113,0.25);color:var(--success);}
        @keyframes flashIn{from{opacity:0;transform:translateY(-10px);}to{opacity:1;transform:translateY(0);}}

        /* Form Styles */
        .oaForm{background:var(--card);border:1px solid var(--border);border-radius:16px;padding:32px;max-width:700px;}
        .formGroup{margin-bottom:20px;}
        .formGroup label{display:block;font-size:12px;font-weight:700;color:var(--dim);text-transform:uppercase;letter-spacing:1px;margin-bottom:8px;}
        .formGroup input,.formGroup select,.formGroup textarea{width:100%;padding:12px 16px;background:rgba(10,10,30,0.6);border:1px solid var(--border);border-radius:10px;color:var(--text);font-size:14px;font-family:inherit;transition:all .2s;outline:none;}
        .formGroup input:focus,.formGroup select:focus,.formGroup textarea:focus{border-color:rgba(201,162,39,0.5);box-shadow:0 0 0 3px rgba(201,162,39,0.08);}
        .formGroup textarea{resize:vertical;min-height:80px;}
        .formGroup select{cursor:pointer;}
        .formRow{display:grid;grid-template-columns:1fr 1fr;gap:16px;}
        .formActions{display:flex;gap:12px;justify-content:flex-end;margin-top:28px;padding-top:20px;border-top:1px solid var(--border);}
        .btnSubmit{padding:12px 28px;background:linear-gradient(135deg,var(--gold),#b08a20);color:#0a0a14;border:none;border-radius:10px;font-size:14px;font-weight:700;font-family:inherit;cursor:pointer;transition:all .2s;}
        .btnSubmit:hover{transform:translateY(-2px);box-shadow:0 4px 16px rgba(201,162,39,0.35);}
        .btnCancel{padding:12px 28px;background:rgba(255,255,255,0.04);border:1px solid var(--border);color:var(--dim);border-radius:10px;font-size:14px;font-weight:600;text-decoration:none;transition:all .2s;display:inline-flex;align-items:center;}
        .btnCancel:hover{color:var(--text);background:rgba(255,255,255,0.06);}

        .footer{text-align:center;padding:32px 20px;color:var(--muted);font-size:11px;}
    </style>
</head>
<body>
    <header class="topbar">
        <div class="topbar-left">
            <a href="/OA_HTML/AppsHome.jsp" class="topbar-logo">🐉 Antaboga</a>
            <div class="topbar-sep"></div>
            <div class="breadcrumb">
                <a href="/OA_HTML/AppsHome.jsp">Home</a>
                <span>›</span>
                <span class="current">${moduleTitle}</span>
            </div>
        </div>
        <div class="topbar-right">
            <span style="font-size:12px;color:var(--dim);">${userFullName} (${userRole})</span>
            <a href="/OA_HTML/AppsLogout.jsp" class="logout">Logout</a>
        </div>
    </header>

    <main class="page">
        <div class="page-header">
            <div>
                <h1>${moduleTitle}</h1>
                <p>Modul ${moduleTitle} — Antaboga ERP</p>
            </div>
        </div>

        <nav class="mod-tabs">
            <a href="/OA_HTML/RF.jsp?module=dashboard" class="mod-tab ${currentModule}">📊 Dashboard</a>
            <a href="/OA_HTML/RF.jsp?module=students" class="mod-tab">🎓 Siswa</a>
            <a href="/OA_HTML/RF.jsp?module=teachers" class="mod-tab">👨‍🏫 Guru</a>
            <a href="/OA_HTML/RF.jsp?module=academic" class="mod-tab">📚 Akademik</a>
            <a href="/OA_HTML/RF.jsp?module=finance" class="mod-tab">💰 Keuangan</a>
            <a href="/OA_HTML/RF.jsp?module=attendance" class="mod-tab">📋 Absensi</a>
            <a href="/OA_HTML/RF.jsp?function_id=report" class="mod-tab">🖥️ Desktop</a>
        </nav>

        <div class="module-content">
            ${flashMessage}
            ${moduleContent}
        </div>
    </main>

    <div class="footer">
        🐉 ${appName} v${appVersion} &middot; ${serverInfo} &middot; ${timestamp}
    </div>

    <script>
        // Highlight active tab
        var tabs = document.querySelectorAll('.mod-tab');
        var current = '${currentModule}';
        tabs.forEach(function(tab) {
            var href = tab.getAttribute('href') || '';
            if (href.indexOf('module=' + current) !== -1) {
                tab.classList.add('active');
            }
        });
    </script>
</body>
</html>
