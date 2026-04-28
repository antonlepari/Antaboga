<%-- AppsLocalLogin.jsp — Antaboga ERP Login Page --%>
<%-- Rendered by Java TemplateEngine, not a servlet container --%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="robots" content="noindex, nofollow">
    <title>${appName} — Login</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg: #060614;
            --card: rgba(16, 16, 44, 0.85);
            --border: rgba(201, 162, 39, 0.12);
            --border-focus: rgba(201, 162, 39, 0.5);
            --gold: #c9a227;
            --gold-light: #f7d06b;
            --text: #e0e0ee;
            --text-dim: #6a6a8a;
            --danger: #e74c3c;
            --radius: 14px;
        }
        * { margin:0; padding:0; box-sizing:border-box; }
        body {
            font-family: 'Inter', sans-serif;
            background: var(--bg);
            color: var(--text);
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            -webkit-font-smoothing: antialiased;
            overflow: hidden;
        }
        .bg-effects {
            position: fixed; inset: 0; z-index: 0; pointer-events: none;
            background:
                radial-gradient(ellipse at 30% 20%, rgba(123,47,247,0.07) 0%, transparent 50%),
                radial-gradient(ellipse at 70% 80%, rgba(201,162,39,0.05) 0%, transparent 50%);
        }
        .bg-grid {
            position: fixed; inset: 0; z-index: 0; pointer-events: none;
            background-image:
                linear-gradient(rgba(201,162,39,0.03) 1px, transparent 1px),
                linear-gradient(90deg, rgba(201,162,39,0.03) 1px, transparent 1px);
            background-size: 60px 60px;
        }
        .glow-orb {
            position: fixed; width: 500px; height: 500px;
            top: 50%; left: 50%; transform: translate(-50%, -50%);
            background: radial-gradient(circle, rgba(201,162,39,0.04) 0%, transparent 70%);
            animation: orbPulse 5s ease-in-out infinite;
            z-index: 0;
        }
        @keyframes orbPulse {
            0%,100% { transform: translate(-50%,-50%) scale(1); opacity:.4; }
            50% { transform: translate(-50%,-50%) scale(1.15); opacity:.8; }
        }

        .login-wrapper { position: relative; z-index: 2; width: 100%; max-width: 440px; padding: 24px; }

        .login-card {
            background: var(--card);
            backdrop-filter: blur(24px);
            -webkit-backdrop-filter: blur(24px);
            border: 1px solid var(--border);
            border-radius: 24px;
            padding: 52px 44px 40px;
            box-shadow: 0 8px 60px rgba(0,0,0,0.5), 0 0 40px rgba(201,162,39,0.05);
            animation: cardIn 0.7s cubic-bezier(0.16, 1, 0.3, 1);
        }
        @keyframes cardIn {
            from { opacity:0; transform: translateY(30px) scale(0.97); }
            to { opacity:1; transform: translateY(0) scale(1); }
        }

        .logo-area { text-align: center; margin-bottom: 36px; }
        .dragon-svg {
            width: 76px; height: 76px; margin-bottom: 16px;
            filter: drop-shadow(0 0 24px rgba(201,162,39,0.25));
            animation: dragonFloat 3s ease-in-out infinite;
        }
        @keyframes dragonFloat {
            0%,100% { transform: translateY(0); }
            50% { transform: translateY(-6px); }
        }
        .logo-area h1 {
            font-size: 30px; font-weight: 800; letter-spacing: -0.5px;
            background: linear-gradient(135deg, var(--gold), var(--gold-light));
            -webkit-background-clip: text; -webkit-text-fill-color: transparent;
            background-clip: text;
        }
        .logo-area .subtitle {
            font-size: 11px; color: var(--text-dim); letter-spacing: 3px;
            text-transform: uppercase; margin-top: 6px;
        }

        .form-field { margin-bottom: 22px; }
        .form-field label {
            display: block; font-size: 11px; font-weight: 700;
            color: var(--text-dim); text-transform: uppercase;
            letter-spacing: 1.5px; margin-bottom: 8px;
        }
        .form-field input {
            width: 100%; padding: 15px 18px;
            background: rgba(10,10,30,0.6);
            border: 1px solid var(--border);
            border-radius: var(--radius);
            color: var(--text); font-size: 14px; font-family: inherit;
            transition: all 0.3s ease; outline: none;
        }
        .form-field input:focus {
            border-color: var(--border-focus);
            box-shadow: 0 0 0 3px rgba(201,162,39,0.1), 0 2px 12px rgba(0,0,0,0.2);
        }
        .form-field input::placeholder { color: rgba(100,100,140,0.5); }

        .error-box {
            background: rgba(231,76,60,0.08);
            border: 1px solid rgba(231,76,60,0.25);
            color: var(--danger);
            padding: 12px 16px;
            border-radius: 10px;
            font-size: 13px;
            margin-bottom: 18px;
            animation: shake 0.4s ease;
        }
        @keyframes shake {
            0%,100% { transform: translateX(0); }
            25% { transform: translateX(-5px); }
            75% { transform: translateX(5px); }
        }

        .btn-login {
            width: 100%; padding: 15px 24px;
            background: linear-gradient(135deg, var(--gold), #b08a20);
            border: none; border-radius: var(--radius);
            color: #0a0a14; font-size: 15px; font-weight: 700;
            font-family: inherit; cursor: pointer;
            display: flex; align-items: center; justify-content: center; gap: 10px;
            transition: all 0.3s ease;
            position: relative; overflow: hidden;
        }
        .btn-login::before {
            content: ''; position: absolute; inset: 0;
            background: linear-gradient(135deg, transparent 0%, rgba(255,255,255,0.15) 50%, transparent 100%);
            transform: translateX(-100%);
            transition: transform 0.5s ease;
        }
        .btn-login:hover { transform: translateY(-2px); box-shadow: 0 6px 24px rgba(201,162,39,0.35); }
        .btn-login:hover::before { transform: translateX(100%); }
        .btn-login:active { transform: translateY(0); }
        .btn-login svg { width: 18px; height: 18px; }

        .footer-info {
            text-align: center; margin-top: 28px;
            padding-top: 20px; border-top: 1px solid rgba(255,255,255,0.04);
        }
        .footer-info p { font-size: 11px; color: var(--text-dim); }
        .footer-info .server-tag {
            display: inline-block; margin-top: 8px;
            padding: 4px 12px; background: rgba(201,162,39,0.06);
            border: 1px solid rgba(201,162,39,0.1);
            border-radius: 20px; font-size: 10px;
            color: var(--gold); letter-spacing: 0.5px;
        }

        .alt-login {
            text-align: center; margin-top: 16px;
        }
        .alt-login a {
            color: var(--gold); text-decoration: none; font-size: 13px; font-weight: 500;
            transition: opacity 0.2s;
        }
        .alt-login a:hover { opacity: 0.7; }
    </style>
</head>
<body>
    <div class="bg-effects"></div>
    <div class="bg-grid"></div>
    <div class="glow-orb"></div>

    <div class="login-wrapper">
        <div class="login-card">
            <div class="logo-area">
                <svg viewBox="0 0 80 80" class="dragon-svg">
                    <defs>
                        <linearGradient id="g1" x1="0%" y1="0%" x2="100%" y2="100%">
                            <stop offset="0%" stop-color="#c9a227"/>
                            <stop offset="100%" stop-color="#f7d06b"/>
                        </linearGradient>
                    </defs>
                    <circle cx="40" cy="40" r="36" fill="none" stroke="url(#g1)" stroke-width="1.5" opacity="0.25"/>
                    <circle cx="40" cy="40" r="28" fill="none" stroke="url(#g1)" stroke-width="0.5" opacity="0.15"/>
                    <path d="M25 52 Q30 24 40 18 Q50 24 55 52 Q50 46 40 44 Q30 46 25 52Z" fill="url(#g1)" opacity="0.85"/>
                    <path d="M32 36 Q36 14 40 10 Q44 14 48 36" fill="none" stroke="url(#g1)" stroke-width="1.5"/>
                    <circle cx="35" cy="33" r="2.5" fill="#060614"/>
                    <circle cx="45" cy="33" r="2.5" fill="#060614"/>
                    <circle cx="35.5" cy="32.5" r="0.8" fill="#f7d06b"/>
                    <circle cx="45.5" cy="32.5" r="0.8" fill="#f7d06b"/>
                    <path d="M18 50 Q21 40 27 37" fill="none" stroke="url(#g1)" stroke-width="1.5" stroke-linecap="round"/>
                    <path d="M62 50 Q59 40 53 37" fill="none" stroke="url(#g1)" stroke-width="1.5" stroke-linecap="round"/>
                    <path d="M22 54 Q20 48 24 44" fill="none" stroke="url(#g1)" stroke-width="1" opacity="0.5"/>
                    <path d="M58 54 Q60 48 56 44" fill="none" stroke="url(#g1)" stroke-width="1" opacity="0.5"/>
                </svg>
                <h1>${appName}</h1>
                <p class="subtitle">Education Resource Planning</p>
            </div>

            <form method="POST" action="/OA_HTML/AppsLocalLogin.jsp" autocomplete="off">
                ${errorMessage}

                <div class="form-field">
                    <label for="usernameField">Username</label>
                    <input type="text" id="usernameField" name="usernameField"
                           value="${lastUsername}" required autofocus
                           placeholder="Masukkan username Anda">
                </div>

                <div class="form-field">
                    <label for="passwordField">Password</label>
                    <input type="password" id="passwordField" name="passwordField"
                           required placeholder="Masukkan password Anda">
                </div>

                <button type="submit" class="btn-login">
                    <span>Masuk ke Sistem</span>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                        <path d="M5 12h14M12 5l7 7-7 7"/>
                    </svg>
                </button>
            </form>

            <div class="alt-login">
                <a href="/">← Kembali ke Web Launcher</a>
            </div>

            <div class="footer-info">
                <p>🐉 Powered by Kamsib &middot; ${appName} v${appVersion}</p>
                <span class="server-tag">${serverInfo}</span>
            </div>
        </div>
    </div>
</body>
</html>
