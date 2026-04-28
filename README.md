<div align="center">

# 🐉 Antaboga ERP

### *Education Resource Planning*

**Sistem manajemen pendidikan terpadu berbasis Java SE 8**
**dengan Web Launcher, JSP-Style Pages, dan Java Web Start Desktop Client**

[![Java](https://img.shields.io/badge/Java-SE%208-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)
[![Security](https://img.shields.io/badge/Security-Hardened-2ecc71?style=for-the-badge&logo=shields&logoColor=white)](SECURITY.md)
[![Version](https://img.shields.io/badge/Version-1.2.0-c9a227?style=for-the-badge)](#)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey?style=for-the-badge)](#)

---

*Antaboga (ꦄꦤ꧀ꦠꦧꦺꦴꦒ) — Sang naga penjaga dunia bawah dalam mitologi Jawa.*
*Seperti Antaboga yang menjaga pondasi dunia, ERP ini menjaga pondasi administrasi pendidikan Anda.*

</div>

---

## 📖 Tentang Antaboga

**Antaboga** adalah aplikasi **Enterprise Resource Planning (ERP)** khusus untuk organisasi pendidikan — terinspirasi oleh arsitektur [Oracle E-Business Suite](https://www.oracle.com/applications/ebusiness/) namun dirancang ringan untuk sekolah, pesantren, lembaga kursus, dan organisasi pendidikan lainnya di Indonesia.

Antaboga dibangun sepenuhnya dengan **Java SE 8** tanpa framework berat. Seluruh logika bisnis diproses di Java — browser hanya berfungsi sebagai launcher dan tampilan.

### Mengapa Antaboga?

| Masalah | Solusi Antaboga |
|---------|-----------------|
| ERP komersial terlalu mahal | Open source, gratis, MIT License |
| Butuh server terpisah (Tomcat, JBoss) | Embedded HTTP server, single JAR |
| Butuh database server (MySQL, PostgreSQL) | H2 embedded, zero-config |
| Sulit di-deploy | `java -jar antaboga-1.2.0.jar` — selesai |
| Tidak ada fitur desktop | Java Web Start (JNLP) untuk native reporting |

---

## ✨ Fitur Utama

### 📦 Modul ERP

| Modul | Deskripsi | Akses |
|:---:|---|---|
| 📊 **Dashboard** | Ringkasan statistik real-time: siswa, guru, kelas, keuangan | Web + JSP |
| 🎓 **Manajemen Siswa** | CRUD lengkap, pencarian, pagination, data orang tua | Web + JSP |
| 👨‍🏫 **Manajemen Guru** | Kelola data guru, spesialisasi, kualifikasi | Web + JSP |
| 📚 **Akademik** | Kelas, mata pelajaran, tahun akademik, penilaian | Web + JSP |
| 💰 **Keuangan** | Invoice, pembayaran, jenis biaya, laporan keuangan | Web + JSP |
| 📋 **Absensi** | Rekam kehadiran harian per siswa per kelas | Web + JSP |
| 🖥️ **Desktop Report** | Laporan native via Java Web Start (JNLP) | Desktop |

### 🔒 Keamanan

| Fitur | Detail |
|-------|--------|
| Password Hashing | BCrypt 12 rounds — tidak pernah menyimpan plaintext |
| Default Password | Random 16-char via `SecureRandom` — tidak hardcoded |
| Session Management | Token random 48-char, cookie HttpOnly + SameSite=Strict |
| CSRF Protection | Token unik per-session pada setiap POST/PUT/DELETE |
| SQL Injection | Prepared statements di 100% query |
| XSS Prevention | Output encoding & input sanitization |
| Rate Limiting | Pembatasan request per IP per menit |
| Security Headers | X-Content-Type-Options, X-Frame-Options, X-XSS-Protection |
| Directory Traversal | Pencegahan akses `../` pada static files |
| Audit Log | Pencatatan semua aksi CRUD + login/logout |
| JAR Signing | Self-signed certificate untuk JNLP security |

---

## 🏗️ Arsitektur

Antaboga menggunakan arsitektur **3-tier** dengan tiga mode akses yang berbeda:

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                       │
│                                                             │
│   🚀 Web Launcher (SPA)      📄 JSP-Style Pages              │
│   Single Page Application    Server-rendered HTML           │
│   AJAX + REST API            Traditional form POST          │
│   Port 1337 (/)              Port 1337 (/OA_HTML/*.jsp)     │
│                                                             │
│   🖥️ Java Desktop Client                                    │
│   Swing UI via JNLP          Native OS integration          │
│   Launched from browser       Runs locally on user machine  │
│   (/OA_HTML/runforms.jsp)    (JavawsLauncher)               │
└───────────────────────┬─────────────────────────────────────┘
                        │ HTTP (port 1337)
┌───────────────────────▼─────────────────────────────────────┐
│                    APPLICATION LAYER                        │
│                    Java SE 8 Backend                        │
│                                                             │
│   ┌─────────────┐ ┌─────────────┐ ┌──────────────────────┐  │
│   │ AuthHandler │ │ Module      │ │ Security Layer       │  │
│   │ Session     │ │ Handlers    │ │ BCrypt, CSRF, Rate   │  │
│   │ Manager     │ │ (7 modules) │ │ Limit, Audit, XSS    │  │
│   └──────┬──────┘ └──────┬──────┘ └──────────────────────┘  │
│          │               │                                  │
│   ┌──────▼───────┐ ┌─────▼──────┐ ┌──────────────────────┐  │
│   │ Template     │ │ JNLP       │ │ Static File          │  │
│   │ Engine       │ │ Handler    │ │ Handler              │  │
│   │ (JSP render) │ │ (Desktop)  │ │ (CSS/JS/SVG)         │  │
│   └──────────────┘ └────────────┘ └──────────────────────┘  │
└───────────────────────┬─────────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────────┐
│                      DATA LAYER                             │
│                                                             │
│   ┌──────────────────────────────────────────────────────┐  │
│   │              H2 Embedded Database                    │  │
│   │   • Zero-config, no separate server needed           │  │
│   │   • Auto-schema creation on first run                │  │
│   │   • Stored in ./data/antaboga.mv.db                  │  │
│   │   • 12 tables: users, students, teachers, classes,   │  │
│   │     subjects, grades, fee_types, invoices, payments, │  │
│   │     attendance, sessions, audit_log                  │  │
│   └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Tiga Mode Akses

#### 1. 🚀 Web Launcher (SPA)
Antarmuka modern berbasis Single Page Application. Semua interaksi melalui REST API dengan AJAX. Cocok untuk pengguna yang menginginkan pengalaman aplikasi web modern.

#### 2. 📄 JSP-Style Pages
Halaman server-rendered yang mengikuti pola URL Oracle E-Business Suite (`/OA_HTML/*.jsp`). Menggunakan template engine custom yang memproses placeholder `${variable}`. Mendukung CRUD via form POST — cocok untuk pengguna yang familiar dengan ERP tradisional.

#### 3. 🖥️ Java Desktop Client (JNLP)
Aplikasi Swing native yang diluncurkan dari browser via Java Web Start. Ketika pengguna mengklik menu **Desktop Report**, browser akan menampilkan popup **"Open JavawsLauncher?"** → aplikasi Java berjalan di mesin lokal, login ke server via API, dan menampilkan laporan lengkap dengan opsi export ke TXT.

---

## 🚀 Quick Start

### Prasyarat

- **Java SE 8** (JDK 8 / OpenJDK 8)
- **Maven 3.6+**

### Cara Cepat (macOS)

```bash
git clone https://github.com/kamsib/antaboga.git
cd antaboga
chmod +x run.sh
./run.sh
```

Script `run.sh` otomatis:
1. Cek & install Java 8 via Homebrew
2. Cek & install Maven via Homebrew
3. Build project dengan Maven
4. Generate self-signed certificate (untuk JNLP)
5. Sign JAR dengan `jarsigner`
6. Jalankan server

### Cara Manual

```bash
# Clone
git clone https://github.com/kamsib/antaboga.git
cd antaboga

# Build
mvn clean package -DskipTests

# Generate certificate & sign JAR (untuk JNLP Desktop)
keytool -genkeypair -alias antaboga -keyalg RSA -keysize 2048 \
  -validity 3650 -keystore config/antaboga.jks \
  -storepass antaboga-kamsib-erp -keypass antaboga-kamsib-erp \
  -dname "CN=Antaboga ERP, OU=Kamsib, O=Kamsib, L=Jakarta, C=ID" -noprompt

jarsigner -keystore config/antaboga.jks \
  -storepass antaboga-kamsib-erp -keypass antaboga-kamsib-erp \
  target/antaboga-1.2.0.jar antaboga

# Jalankan
java -jar target/antaboga-1.2.0.jar
```

### Akses Aplikasi

| Halaman | URL |
|---------|-----|
| 🚀 **Web Launcher** (SPA) | http://localhost:1337 |
| 📄 **JSP Login** | http://localhost:1337/OA_HTML/AppsLocalLogin.jsp |
| 🏠 **JSP Home** | http://localhost:1337/OA_HTML/AppsHome.jsp |
| 🖥️ **Desktop Report** | http://localhost:1337/OA_HTML/RF.jsp?function_id=report |

### 🔑 Login

| Field | Nilai |
|-------|-------|
| Username | `admin` |
| Password | *(di-generate random saat pertama kali — lihat console)* |

> **Penting:** Password admin di-generate dengan `SecureRandom` (16 karakter: huruf besar + kecil + angka + simbol). Hanya ditampilkan **sekali** di terminal saat pertama kali server dijalankan. **Catat segera!**

---

## 🌐 Endpoint Lengkap

### Web Launcher REST API

| URL | Method | Deskripsi |
|-----|--------|-----------|
| `/api/auth/login` | POST | Login (JSON: username + password) |
| `/api/auth/me` | GET | Info user yang sedang login |
| `/api/auth/logout` | POST | Logout & hapus session |
| `/api/dashboard` | GET | Statistik dashboard (total siswa, guru, dll) |
| `/api/students` | GET | Daftar semua siswa |
| `/api/students` | POST | Tambah siswa baru |
| `/api/students/{id}` | GET | Detail satu siswa |
| `/api/students/{id}` | PUT | Edit data siswa |
| `/api/students/{id}` | DELETE | Hapus siswa |
| `/api/teachers` | GET/POST | Daftar & tambah guru |
| `/api/teachers/{id}` | GET/PUT/DELETE | Detail, edit, hapus guru |
| `/api/academic/classes` | GET/POST | Kelas |
| `/api/academic/subjects` | GET/POST | Mata pelajaran |
| `/api/academic/grades` | GET/POST | Nilai siswa |
| `/api/finance/invoices` | GET/POST | Invoice |
| `/api/finance/payments` | GET/POST | Pembayaran |
| `/api/attendance` | GET/POST | Absensi |

### OA_HTML JSP-Style Pages

| URL | Deskripsi |
|-----|-----------|
| `/OA_HTML/AppsLocalLogin.jsp` | Halaman login (form POST) |
| `/OA_HTML/AppsHome.jsp` | Home navigator dengan statistik & module cards |
| `/OA_HTML/RF.jsp?module=students` | Modul siswa (tabel + CRUD form) |
| `/OA_HTML/RF.jsp?module=teachers` | Modul guru (tabel + CRUD form) |
| `/OA_HTML/RF.jsp?module=academic` | Modul akademik |
| `/OA_HTML/RF.jsp?module=finance` | Modul keuangan |
| `/OA_HTML/RF.jsp?module=attendance` | Modul absensi |
| `/OA_HTML/RF.jsp?function_id=report` | Daftar fungsi desktop (JNLP) |
| `/OA_HTML/runforms.jsp?resp_app=SQLAP&resp_key=...&start_func=...` | Launch JavawsLauncher |
| `/OA_HTML/AppsLogout.jsp` | Logout & redirect |

> Semua halaman `.jsp` di-render oleh Java backend via `TemplateEngine` — **bukan** servlet container. URL pattern mengikuti konvensi Oracle E-Business Suite.

### Java Web Start (JNLP)

| URL | Deskripsi |
|-----|-----------|
| `/launch/antaboga.jnlp` | JNLP file → trigger browser popup "Open JavawsLauncher?" |
| `/launch/antaboga-desktop.jar` | Signed JAR untuk desktop client |

### Alur JNLP Desktop

```
1. User buka  → /OA_HTML/RF.jsp?function_id=report
2. Klik "Launch" pada fungsi    → /OA_HTML/runforms.jsp?resp_app=SQLAP&resp_key=99999_GURU_KITA&start_func=REPORT_GURU
3. Browser popup               → "localhost:1337 wants to open JavawsLauncher"
4. User klik "Open"            → javaws download signed JAR & launch Swing app
5. Desktop app muncul          → Login dialog → Fetch data via API → Tampilkan laporan
```

---

## ⚙️ Konfigurasi

### File Konfigurasi

```bash
cp config/antaboga.properties.example config/antaboga.properties
```

> ⚠️ `config/antaboga.properties` sudah di `.gitignore` — **tidak akan ter-commit**.

### Environment Variables

| Variable | Deskripsi | Default |
|----------|-----------|---------|
| `ANTABOGA_PORT` | Port server | `1337` |
| `ANTABOGA_HOST` | Bind address | `127.0.0.1` |
| `ANTABOGA_DB_PATH` | Path database H2 | `./data/antaboga` |
| `ANTABOGA_DB_USER` | Username database | `antaboga` |
| `ANTABOGA_DB_PASS` | Password database | *(wajib diset)* |

```bash
# Contoh menjalankan dengan konfigurasi custom
ANTABOGA_PORT=9090 ANTABOGA_DB_PASS=MyS3cur3P4ss! java -jar target/antaboga-1.2.0.jar
```

### File Sensitif yang TIDAK Di-commit

| File/Folder | Status | Alasan |
|-------------|--------|--------|
| `config/antaboga.properties` | 🚫 `.gitignore` | Berisi password database |
| `config/antaboga.jks` | 🚫 `.gitignore` | Keystore untuk JAR signing |
| `data/` | 🚫 `.gitignore` | Database H2 |
| `*.key, *.pem, *.p12, *.jks` | 🚫 `.gitignore` | File sertifikat/kunci |
| `config/antaboga.properties.example` | ✅ Committed | Hanya placeholder |
| `run.sh` | ✅ Committed | Tidak mengandung credential |

> 📄 Lihat [SECURITY.md](SECURITY.md) untuk kebijakan keamanan lengkap dan cara melaporkan kerentanan.

---

## 📁 Struktur Project

```
antaboga/
├── src/main/java/id/kamsib/antaboga/
│   ├── App.java                        # Entry point, banner, shutdown hook
│   ├── config/
│   │   └── AppConfig.java              # Konfigurasi (env > file > defaults)
│   ├── db/
│   │   └── DatabaseManager.java        # H2 DB, auto-schema, random password seed
│   ├── server/
│   │   ├── WebServer.java              # Embedded HTTP server, route registry
│   │   ├── HttpUtil.java               # Request/response utilities, cookie parser
│   │   ├── StaticFileHandler.java      # Static file serving (CSS/JS/SVG)
│   │   ├── TemplateEngine.java         # JSP-like ${var} template engine
│   │   ├── OAHtmlHandler.java          # /OA_HTML/ handler (login, CRUD, forms)
│   │   └── JnlpHandler.java           # JNLP file generator & JAR server
│   ├── desktop/
│   │   └── AntabogaDesktop.java        # Swing desktop client (JNLP)
│   ├── security/
│   │   ├── PasswordHasher.java         # BCrypt 12-round hashing
│   │   ├── SessionManager.java         # Session, CSRF token, rate limiting
│   │   └── InputValidator.java         # Input validation, XSS sanitization
│   └── modules/
│       ├── auth/AuthHandler.java       # Login/logout/session API
│       ├── dashboard/DashboardHandler.java  # Statistics API
│       ├── student/StudentHandler.java      # Student CRUD API
│       ├── teacher/TeacherHandler.java      # Teacher CRUD API
│       ├── academic/AcademicHandler.java    # Classes/subjects/grades API
│       ├── finance/FinanceHandler.java      # Invoices/payments API
│       └── attendance/AttendanceHandler.java # Attendance API
├── src/main/resources/
│   ├── web/                            # SPA Web Launcher
│   │   ├── index.html                  # Single Page Application
│   │   ├── favicon.svg                 # Dragon favicon (SVG)
│   │   ├── css/style.css               # Dark theme, glassmorphism
│   │   └── js/app.js                   # SPA logic, API client
│   └── oa_html/                        # JSP Templates
│       ├── AppsLocalLogin.jsp          # Login page (form POST)
│       ├── AppsHome.jsp                # Home navigator + stats
│       ├── RF.jsp                      # Responsibility Function (module viewer)
│       ├── runforms.jsp                # JNLP launcher page
│       ├── OA.jsp                      # Dynamic page renderer
│       ├── 404.jsp                     # Not found page
│       └── 500.jsp                     # Server error page
├── config/
│   └── antaboga.properties.example     # Configuration template
├── run.sh                              # Auto-setup & run (macOS)
├── pom.xml                             # Maven build config
├── LICENSE                             # MIT License
├── SECURITY.md                         # Security policy
└── README.md
```

---

## 🛠️ Teknologi

| Komponen | Teknologi | Keterangan |
|----------|-----------|------------|
| Runtime | Java SE 8 | Kompatibel dengan OpenJDK 8 |
| HTTP Server | `com.sun.net.httpserver` | Built-in JDK, tanpa dependency |
| Database | H2 1.4.200 | Embedded SQL database |
| JSON | Gson 2.8.9 | Serialization/deserialization |
| Password | jBCrypt 0.4 | BCrypt hashing |
| Template | Custom `TemplateEngine` | JSP-like `${var}` rendering |
| Desktop | Swing + Java Web Start | Native desktop via JNLP |
| JAR Signing | `keytool` + `jarsigner` | Self-signed certificate |
| Build | Maven + Shade Plugin | Single fat JAR output |
| Frontend | Vanilla HTML/CSS/JS | Dark theme, glassmorphism, animations |

### Dependency Total: **3 library**
```xml
<dependencies>
    <dependency>com.h2database:h2:1.4.200</dependency>
    <dependency>com.google.code.gson:gson:2.8.9</dependency>
    <dependency>org.mindrot:jbcrypt:0.4</dependency>
</dependencies>
```

---

## 📸 Screenshots

### URL Pattern (Oracle EBS Style)
```
http://localhost:1337/OA_HTML/AppsLocalLogin.jsp          ← Login
http://localhost:1337/OA_HTML/AppsHome.jsp                ← Home Navigator
http://localhost:1337/OA_HTML/RF.jsp?module=students      ← Modul Siswa
http://localhost:1337/OA_HTML/RF.jsp?function_id=report   ← Desktop Functions
http://localhost:1337/OA_HTML/runforms.jsp?start_func=... ← Launch JNLP
```

---

## 🤝 Kontribusi

1. Fork repository ini
2. Buat branch fitur (`git checkout -b fitur/nama-fitur`)
3. Commit perubahan (`git commit -m 'Tambah fitur baru'`)
4. Push ke branch (`git push origin fitur/nama-fitur`)
5. Buat Pull Request

**Sebelum commit**, pastikan:
- Jalankan `git diff --cached` untuk cek tidak ada credential
- Jangan commit `config/antaboga.properties` atau `config/antaboga.jks`
- Pastikan build berhasil: `mvn clean package -DskipTests`

---

## 📜 Lisensi

Distributed under the **MIT License**. Lihat [LICENSE](LICENSE) untuk informasi lebih lanjut.

---

<div align="center">

**Dibuat dengan 🐉 oleh [Kamsib](https://github.com/kamsib)**

*Antaboga v1.2.0 — Menjaga pondasi pendidikan Indonesia*

</div>
