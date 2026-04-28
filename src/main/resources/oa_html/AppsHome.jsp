<%-- AppsHome.jsp — Antaboga ERP Home Navigator --%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="robots" content="noindex, nofollow">
    <title>${appName} — Home</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg:#0a0a1a; --bg2:#111128; --card:rgba(22,22,58,0.7); --border:rgba(201,162,39,0.12);
            --gold:#c9a227; --gold-l:#f7d06b; --text:#e0e0ee; --dim:#6a6a8a; --muted:#444466;
            --success:#2ecc71; --warning:#f39c12; --danger:#e74c3c; --info:#3498db;
        }
        *{margin:0;padding:0;box-sizing:border-box;}
        body{font-family:'Inter',sans-serif;background:var(--bg);color:var(--text);min-height:100vh;-webkit-font-smoothing:antialiased;}

        .topbar{
            background:var(--bg2);border-bottom:1px solid var(--border);
            padding:0 32px;height:60px;display:flex;align-items:center;justify-content:space-between;
            position:sticky;top:0;z-index:100;backdrop-filter:blur(12px);
        }
        .topbar-left{display:flex;align-items:center;gap:14px;}
        .topbar-logo{
            font-size:20px;font-weight:800;
            background:linear-gradient(135deg,var(--gold),var(--gold-l));
            -webkit-background-clip:text;-webkit-text-fill-color:transparent;background-clip:text;
        }
        .topbar-sep{width:1px;height:28px;background:var(--border);}
        .topbar-nav{display:flex;gap:4px;}
        .topbar-nav a{
            padding:8px 16px;border-radius:8px;color:var(--dim);text-decoration:none;
            font-size:13px;font-weight:500;transition:all .2s;
        }
        .topbar-nav a:hover{color:var(--text);background:rgba(255,255,255,0.04);}
        .topbar-nav a.active{color:var(--gold);background:rgba(201,162,39,0.08);}
        .topbar-right{display:flex;align-items:center;gap:16px;}
        .user-pill{
            display:flex;align-items:center;gap:10px;
            padding:6px 16px 6px 6px;border-radius:24px;
            background:rgba(201,162,39,0.06);border:1px solid var(--border);
        }
        .user-pill .avatar{
            width:32px;height:32px;border-radius:50%;
            background:linear-gradient(135deg,#7b2ff7,var(--gold));
            display:flex;align-items:center;justify-content:center;
            font-weight:700;font-size:13px;color:#fff;
        }
        .user-pill .uname{font-size:13px;font-weight:600;}
        .user-pill .urole{font-size:10px;color:var(--dim);}
        .btn-logout{
            padding:8px 16px;border-radius:8px;font-size:12px;font-weight:600;
            background:rgba(231,76,60,0.08);border:1px solid rgba(231,76,60,0.2);
            color:var(--danger);cursor:pointer;text-decoration:none;font-family:inherit;
            transition:all .2s;
        }
        .btn-logout:hover{background:rgba(231,76,60,0.15);}

        .main{max-width:1200px;margin:0 auto;padding:40px 32px;}
        .welcome{margin-bottom:36px;}
        .welcome h1{font-size:28px;font-weight:800;margin-bottom:6px;}
        .welcome p{color:var(--dim);font-size:14px;}

        .stats{display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:16px;margin-bottom:40px;}
        .stat{
            background:var(--card);border:1px solid var(--border);border-radius:16px;
            padding:24px;transition:all .3s;position:relative;overflow:hidden;
        }
        .stat::after{
            content:'';position:absolute;top:0;left:0;right:0;height:2px;
            background:linear-gradient(90deg,var(--gold),#7b2ff7);opacity:0;transition:opacity .3s;
        }
        .stat:hover{transform:translateY(-3px);box-shadow:0 8px 30px rgba(0,0,0,0.3);}
        .stat:hover::after{opacity:1;}
        .stat-icon{font-size:24px;margin-bottom:10px;}
        .stat-val{font-size:28px;font-weight:800;color:var(--gold-l);}
        .stat-label{font-size:12px;color:var(--dim);margin-top:4px;font-weight:500;}

        .modules-title{font-size:16px;font-weight:700;margin-bottom:20px;color:var(--dim);text-transform:uppercase;letter-spacing:2px;font-size:12px;}
        .modules{display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:16px;}
        .mod-card{
            background:var(--card);border:1px solid var(--border);border-radius:16px;
            padding:28px;cursor:pointer;transition:all .3s;text-decoration:none;color:inherit;
            display:block;position:relative;overflow:hidden;
        }
        .mod-card::before{
            content:'';position:absolute;inset:0;
            background:linear-gradient(135deg,rgba(201,162,39,0.03),rgba(123,47,247,0.03));
            opacity:0;transition:opacity .3s;
        }
        .mod-card:hover{transform:translateY(-4px);box-shadow:0 8px 30px rgba(0,0,0,0.3);border-color:rgba(201,162,39,0.25);}
        .mod-card:hover::before{opacity:1;}
        .mod-card .mod-icon{font-size:32px;margin-bottom:14px;display:block;}
        .mod-card .mod-name{font-size:16px;font-weight:700;margin-bottom:6px;}
        .mod-card .mod-desc{font-size:13px;color:var(--dim);line-height:1.5;}
        .mod-card .mod-arrow{
            position:absolute;top:28px;right:28px;color:var(--muted);
            transition:all .3s;font-size:18px;
        }
        .mod-card:hover .mod-arrow{color:var(--gold);transform:translateX(4px);}

        .footer-bar{
            text-align:center;padding:40px 20px 24px;color:var(--muted);font-size:11px;
        }
        .footer-bar span{color:var(--dim);}

        .jnlp-card{border-color:rgba(201,162,39,0.3) !important;background:rgba(201,162,39,0.04) !important;}
        .jnlp-card:hover{border-color:var(--gold) !important;box-shadow:0 8px 30px rgba(201,162,39,0.15) !important;}
        .jnlp-badge{position:absolute;bottom:16px;right:16px;padding:3px 10px;border-radius:20px;font-size:10px;font-weight:800;letter-spacing:1px;background:linear-gradient(135deg,var(--gold),#b08a20);color:#0a0a14;}
    </style>
</head>
<body>
    <header class="topbar">
        <div class="topbar-left">
            <span class="topbar-logo">🐉 ${appName}</span>
            <div class="topbar-sep"></div>
            <nav class="topbar-nav">
                <a href="/OA_HTML/AppsHome.jsp" class="active">Home</a>
                <a href="/OA_HTML/RF.jsp?module=students">Siswa</a>
                <a href="/OA_HTML/RF.jsp?module=teachers">Guru</a>
                <a href="/OA_HTML/RF.jsp?module=academic">Akademik</a>
                <a href="/OA_HTML/RF.jsp?module=finance">Keuangan</a>
                <a href="/OA_HTML/RF.jsp?module=attendance">Absensi</a>
            </nav>
        </div>
        <div class="topbar-right">
            <div class="user-pill">
                <div class="avatar">${userFullName}</div>
                <div>
                    <div class="uname">${userFullName}</div>
                    <div class="urole">${userRole}</div>
                </div>
            </div>
            <a href="/OA_HTML/AppsLogout.jsp" class="btn-logout">Logout</a>
        </div>
    </header>

    <main class="main">
        <div class="welcome">
            <h1>Selamat Datang, ${userFullName}</h1>
            <p>Navigator Antaboga ERP &mdash; Pilih modul untuk memulai</p>
        </div>

        <div class="stats">
            <div class="stat"><div class="stat-icon">🎓</div><div class="stat-val">${totalStudents}</div><div class="stat-label">Total Siswa</div></div>
            <div class="stat"><div class="stat-icon">👨‍🏫</div><div class="stat-val">${totalTeachers}</div><div class="stat-label">Total Guru</div></div>
            <div class="stat"><div class="stat-icon">🏫</div><div class="stat-val">${totalClasses}</div><div class="stat-label">Total Kelas</div></div>
            <div class="stat"><div class="stat-icon">📚</div><div class="stat-val">${totalSubjects}</div><div class="stat-label">Mata Pelajaran</div></div>
            <div class="stat"><div class="stat-icon">💰</div><div class="stat-val">${unpaidInvoices}</div><div class="stat-label">Invoice Belum Bayar</div></div>
        </div>

        <div class="modules-title">Modul Tersedia</div>
        <div class="modules">
            <a href="/OA_HTML/RF.jsp?module=students" class="mod-card">
                <span class="mod-icon">🎓</span>
                <div class="mod-name">Manajemen Siswa</div>
                <div class="mod-desc">Kelola data siswa, pendaftaran, dan informasi orang tua</div>
                <span class="mod-arrow">→</span>
            </a>
            <a href="/OA_HTML/RF.jsp?module=teachers" class="mod-card">
                <span class="mod-icon">👨‍🏫</span>
                <div class="mod-name">Manajemen Guru</div>
                <div class="mod-desc">Kelola data guru, spesialisasi, dan kualifikasi</div>
                <span class="mod-arrow">→</span>
            </a>
            <a href="/OA_HTML/RF.jsp?module=academic" class="mod-card">
                <span class="mod-icon">📚</span>
                <div class="mod-name">Akademik</div>
                <div class="mod-desc">Kelas, mata pelajaran, jadwal, dan penilaian</div>
                <span class="mod-arrow">→</span>
            </a>
            <a href="/OA_HTML/RF.jsp?module=finance" class="mod-card">
                <span class="mod-icon">💰</span>
                <div class="mod-name">Keuangan</div>
                <div class="mod-desc">Invoice, pembayaran, jenis biaya, dan laporan</div>
                <span class="mod-arrow">→</span>
            </a>
            <a href="/OA_HTML/RF.jsp?module=attendance" class="mod-card">
                <span class="mod-icon">📋</span>
                <div class="mod-name">Absensi</div>
                <div class="mod-desc">Rekam dan pantau kehadiran harian siswa</div>
                <span class="mod-arrow">→</span>
            </a>
            <a href="#" onclick="launchDesktop(); return false;" class="mod-card jnlp-card">
                <span class="mod-icon">🖥️</span>
                <div class="mod-name">Desktop Report</div>
                <div class="mod-desc">Buka modul laporan di Java Desktop (via Java Web Start)</div>
                <span class="mod-arrow" style="color:var(--gold);">⬇</span>
                <div class="jnlp-badge">JNLP</div>
            </a>
            <a href="/" class="mod-card">
                <span class="mod-icon">🚀</span>
                <div class="mod-name">Web Launcher</div>
                <div class="mod-desc">Buka antarmuka SPA modern dengan fitur lengkap</div>
                <span class="mod-arrow">→</span>
            </a>
        </div>
    </main>

    <div class="footer-bar">
        🐉 ${appName} v${appVersion} &middot; <span>${serverInfo}</span> &middot; ${timestamp}
    </div>

    <script>
        // Show first letter on avatar
        document.querySelectorAll('.avatar').forEach(el => {
            el.textContent = (el.textContent || 'U').trim()[0].toUpperCase();
        });

        // Launch JNLP via protocol handler → triggers browser popup
        function launchDesktop() {
            var jnlpUrl = window.location.protocol + '//' + window.location.host + '/launch/antaboga.jnlp';
            var protoUrl = 'jnlp://' + window.location.host + '/launch/antaboga.jnlp';

            // Create a hidden iframe to trigger the protocol handler
            // This triggers "localhost wants to open this application" popup
            var iframe = document.createElement('iframe');
            iframe.style.display = 'none';
            iframe.src = protoUrl;
            document.body.appendChild(iframe);

            // Fallback: if protocol handler not registered, try direct URL after delay
            setTimeout(function() {
                document.body.removeChild(iframe);
            }, 3000);
        }
    </script>
</body>
</html>
