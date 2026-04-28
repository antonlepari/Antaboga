/**
 * Antaboga ERP — Frontend Application
 * All module functions are executed via Java backend API
 */
(function () {
    'use strict';

    const API = '/api';
    let csrfToken = '';
    let currentModule = 'dashboard';

    // ============ INIT ============
    document.addEventListener('DOMContentLoaded', () => {
        checkSession();
        document.getElementById('loginForm').addEventListener('submit', handleLogin);
        document.getElementById('logoutBtn').addEventListener('click', handleLogout);
        document.querySelectorAll('.menu-item').forEach(item => {
            item.addEventListener('click', () => switchModule(item.dataset.module));
        });
    });

    // ============ AUTH ============
    async function checkSession() {
        try {
            const res = await api('GET', '/auth/me');
            if (res.success) {
                csrfToken = res.user.csrfToken;
                showApp(res.user);
            } else {
                showLogin();
            }
        } catch {
            showLogin();
        }
    }

    async function handleLogin(e) {
        e.preventDefault();
        const errEl = document.getElementById('loginError');
        errEl.style.display = 'none';

        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value;

        if (!username || !password) {
            errEl.textContent = 'Username dan password harus diisi';
            errEl.style.display = 'block';
            return;
        }

        try {
            const res = await api('POST', '/auth/login', { username, password });
            if (res.success) {
                csrfToken = res.csrfToken;
                showApp({ fullName: res.fullName, role: res.role });
                toast('Login berhasil! Selamat datang.', 'success');
            } else {
                errEl.textContent = res.message || 'Login gagal';
                errEl.style.display = 'block';
            }
        } catch (err) {
            errEl.textContent = 'Koneksi ke server gagal';
            errEl.style.display = 'block';
        }
    }

    async function handleLogout() {
        await api('POST', '/auth/logout');
        csrfToken = '';
        showLogin();
        toast('Anda telah keluar', 'success');
    }

    function showLogin() {
        document.getElementById('loginScreen').classList.add('active');
        document.getElementById('appScreen').classList.remove('active');
    }

    function showApp(user) {
        document.getElementById('loginScreen').classList.remove('active');
        document.getElementById('appScreen').classList.add('active');
        document.getElementById('userName').textContent = user.fullName || 'User';
        document.getElementById('userRole').textContent = user.role || 'user';
        document.getElementById('userAvatar').textContent = (user.fullName || 'U')[0].toUpperCase();
        loadModule('dashboard');
    }

    // ============ NAVIGATION ============
    function switchModule(module) {
        currentModule = module;
        document.querySelectorAll('.menu-item').forEach(i => i.classList.remove('active'));
        document.querySelector(`[data-module="${module}"]`).classList.add('active');
        loadModule(module);
    }

    function loadModule(module) {
        const content = document.getElementById('content');
        content.innerHTML = '<div class="loading"><div class="spinner"></div></div>';
        switch (module) {
            case 'dashboard': loadDashboard(); break;
            case 'students': loadStudents(); break;
            case 'teachers': loadTeachers(); break;
            case 'academic': loadAcademic(); break;
            case 'finance': loadFinance(); break;
            case 'attendance': loadAttendance(); break;
            default: content.innerHTML = '<div class="empty-state"><div class="empty-icon">🚧</div><p>Modul dalam pengembangan</p></div>';
        }
    }

    // ============ DASHBOARD ============
    async function loadDashboard() {
        const res = await api('GET', '/dashboard');
        if (!res.success) return;
        const s = res.stats;
        document.getElementById('content').innerHTML = `
            <div class="content-header">
                <div><h2>Dashboard</h2><p>Ringkasan data sekolah</p></div>
            </div>
            <div class="stats-grid">
                ${statCard('🎓', s.totalStudents, 'Total Siswa')}
                ${statCard('👨‍🏫', s.totalTeachers, 'Total Guru')}
                ${statCard('🏫', s.totalClasses, 'Total Kelas')}
                ${statCard('📚', s.totalSubjects, 'Mata Pelajaran')}
                ${statCard('💰', s.unpaidInvoices, 'Invoice Belum Bayar')}
                ${statCard('📅', s.activeYear || '-', 'Tahun Akademik')}
                ${statCard('📋', s.todayAttendance + '%', 'Kehadiran Hari Ini')}
            </div>`;
    }

    function statCard(icon, value, label) {
        return `<div class="stat-card"><div class="stat-icon">${icon}</div><div class="stat-value">${value}</div><div class="stat-label">${label}</div></div>`;
    }

    // ============ STUDENTS ============
    async function loadStudents(page = 1, search = '') {
        const res = await api('GET', `/students?page=${page}&limit=15&search=${encodeURIComponent(search)}`);
        if (!res.success) return;
        const content = document.getElementById('content');
        content.innerHTML = `
            <div class="content-header">
                <div><h2>Manajemen Siswa</h2><p>Kelola data siswa</p></div>
                <button class="btn btn-gold" onclick="window._addStudent()">+ Tambah Siswa</button>
            </div>
            <div class="data-card">
                <div class="data-card-header">
                    <h3>Daftar Siswa (${res.total})</h3>
                    <input class="search-input" placeholder="Cari siswa..." value="${esc(search)}" id="studentSearch">
                </div>
                <table class="data-table">
                    <thead><tr><th>NIS</th><th>Nama</th><th>Gender</th><th>Telepon</th><th>Status</th><th>Aksi</th></tr></thead>
                    <tbody>${res.data.length ? res.data.map(s => `
                        <tr>
                            <td>${esc(s.nis)}</td>
                            <td>${esc(s.fullName)}</td>
                            <td>${esc(s.gender || '-')}</td>
                            <td>${esc(s.phone || '-')}</td>
                            <td><span class="badge badge-success">${esc(s.status)}</span></td>
                            <td>
                                <button class="btn btn-outline btn-sm" onclick="window._editStudent(${s.id})">Edit</button>
                                <button class="btn-danger-sm" onclick="window._deleteStudent(${s.id})">Hapus</button>
                            </td>
                        </tr>`).join('') : '<tr><td colspan="6"><div class="empty-state"><div class="empty-icon">🎓</div><p>Belum ada data siswa</p></div></td></tr>'}</tbody>
                </table>
                ${pagination(page, res.totalPages, 'loadStudentsPage')}
            </div>`;
        document.getElementById('studentSearch').addEventListener('keyup', debounce(e => loadStudents(1, e.target.value), 400));
    }

    window._addStudent = () => showModal('Tambah Siswa', studentForm(), async (data) => {
        const res = await api('POST', '/students', data);
        if (res.success) { closeModal(); loadStudents(); toast(res.message, 'success'); }
        else toast(res.message, 'error');
    });

    window._editStudent = async (id) => {
        const res = await api('GET', `/students/${id}`);
        if (!res.success) return;
        showModal('Edit Siswa', studentForm(res.data), async (data) => {
            const r = await api('PUT', `/students/${id}`, data);
            if (r.success) { closeModal(); loadStudents(); toast(r.message, 'success'); }
            else toast(r.message, 'error');
        });
    };

    window._deleteStudent = async (id) => {
        if (!confirm('Yakin ingin menghapus siswa ini?')) return;
        const res = await api('DELETE', `/students/${id}`);
        if (res.success) { loadStudents(); toast(res.message, 'success'); }
        else toast(res.message, 'error');
    };

    window.loadStudentsPage = (p) => loadStudents(p, document.getElementById('studentSearch')?.value || '');

    function studentForm(d = {}) {
        return `
            <div class="form-row">
                <div class="form-group"><label>NIS *</label><input name="nis" value="${esc(d.nis || '')}" required ${d.nis ? 'readonly' : ''}></div>
                <div class="form-group"><label>Nama Lengkap *</label><input name="fullName" value="${esc(d.fullName || '')}" required></div>
            </div>
            <div class="form-row">
                <div class="form-group"><label>Gender</label><select name="gender"><option value="">--</option><option value="L" ${d.gender === 'L' ? 'selected' : ''}>Laki-laki</option><option value="P" ${d.gender === 'P' ? 'selected' : ''}>Perempuan</option></select></div>
                <div class="form-group"><label>Tempat Lahir</label><input name="birthPlace" value="${esc(d.birthPlace || '')}"></div>
            </div>
            <div class="form-row">
                <div class="form-group"><label>Tanggal Lahir</label><input type="date" name="birthDate" value="${esc(d.birthDate || '')}"></div>
                <div class="form-group"><label>Telepon</label><input name="phone" value="${esc(d.phone || '')}"></div>
            </div>
            <div class="form-group"><label>Email</label><input type="email" name="email" value="${esc(d.email || '')}"></div>
            <div class="form-group"><label>Alamat</label><textarea name="address">${esc(d.address || '')}</textarea></div>
            <div class="form-row">
                <div class="form-group"><label>Nama Orang Tua</label><input name="parentName" value="${esc(d.parentName || '')}"></div>
                <div class="form-group"><label>Telepon Orang Tua</label><input name="parentPhone" value="${esc(d.parentPhone || '')}"></div>
            </div>`;
    }

    // ============ TEACHERS ============
    async function loadTeachers(page = 1, search = '') {
        const res = await api('GET', `/teachers?page=${page}&limit=15&search=${encodeURIComponent(search)}`);
        if (!res.success) return;
        document.getElementById('content').innerHTML = `
            <div class="content-header">
                <div><h2>Manajemen Guru</h2><p>Kelola data guru dan staf</p></div>
                <button class="btn btn-gold" onclick="window._addTeacher()">+ Tambah Guru</button>
            </div>
            <div class="data-card">
                <div class="data-card-header"><h3>Daftar Guru (${res.total})</h3><input class="search-input" placeholder="Cari guru..." value="${esc(search)}" id="teacherSearch"></div>
                <table class="data-table">
                    <thead><tr><th>NIP</th><th>Nama</th><th>Spesialisasi</th><th>Telepon</th><th>Status</th><th>Aksi</th></tr></thead>
                    <tbody>${res.data.length ? res.data.map(t => `<tr><td>${esc(t.nip)}</td><td>${esc(t.fullName)}</td><td>${esc(t.subjectSpecialization || '-')}</td><td>${esc(t.phone || '-')}</td><td><span class="badge badge-success">${esc(t.status)}</span></td><td><button class="btn btn-outline btn-sm" onclick="window._editTeacher(${t.id})">Edit</button> <button class="btn-danger-sm" onclick="window._deleteTeacher(${t.id})">Hapus</button></td></tr>`).join('') : '<tr><td colspan="6"><div class="empty-state"><div class="empty-icon">👨‍🏫</div><p>Belum ada data guru</p></div></td></tr>'}</tbody>
                </table>
            </div>`;
        document.getElementById('teacherSearch').addEventListener('keyup', debounce(e => loadTeachers(1, e.target.value), 400));
    }

    window._addTeacher = () => showModal('Tambah Guru', teacherForm(), async (data) => {
        const res = await api('POST', '/teachers', data);
        if (res.success) { closeModal(); loadTeachers(); toast(res.message, 'success'); } else toast(res.message, 'error');
    });

    window._editTeacher = async (id) => {
        const res = await api('GET', `/teachers/${id}`);
        if (!res.success) return;
        showModal('Edit Guru', teacherForm(res.data), async (data) => {
            const r = await api('PUT', `/teachers/${id}`, data);
            if (r.success) { closeModal(); loadTeachers(); toast(r.message, 'success'); } else toast(r.message, 'error');
        });
    };

    window._deleteTeacher = async (id) => {
        if (!confirm('Yakin ingin menghapus guru ini?')) return;
        const res = await api('DELETE', `/teachers/${id}`);
        if (res.success) { loadTeachers(); toast(res.message, 'success'); } else toast(res.message, 'error');
    };

    function teacherForm(d = {}) {
        return `
            <div class="form-row"><div class="form-group"><label>NIP *</label><input name="nip" value="${esc(d.nip || '')}" required></div><div class="form-group"><label>Nama Lengkap *</label><input name="fullName" value="${esc(d.fullName || '')}" required></div></div>
            <div class="form-row"><div class="form-group"><label>Gender</label><select name="gender"><option value="">--</option><option value="L" ${d.gender === 'L' ? 'selected' : ''}>Laki-laki</option><option value="P" ${d.gender === 'P' ? 'selected' : ''}>Perempuan</option></select></div><div class="form-group"><label>Spesialisasi</label><input name="subjectSpecialization" value="${esc(d.subjectSpecialization || '')}"></div></div>
            <div class="form-row"><div class="form-group"><label>Telepon</label><input name="phone" value="${esc(d.phone || '')}"></div><div class="form-group"><label>Email</label><input name="email" value="${esc(d.email || '')}"></div></div>
            <div class="form-group"><label>Kualifikasi</label><input name="qualification" value="${esc(d.qualification || '')}"></div>
            <div class="form-group"><label>Alamat</label><textarea name="address">${esc(d.address || '')}</textarea></div>`;
    }

    // ============ ACADEMIC ============
    async function loadAcademic() {
        const [classes, subjects, years] = await Promise.all([
            api('GET', '/academic/classes'), api('GET', '/academic/subjects'), api('GET', '/academic/years')
        ]);
        document.getElementById('content').innerHTML = `
            <div class="content-header"><div><h2>Akademik</h2><p>Kelola kelas, mata pelajaran, dan tahun akademik</p></div></div>
            <div class="stats-grid">
                ${statCard('🏫', classes.data?.length || 0, 'Kelas')}
                ${statCard('📚', subjects.data?.length || 0, 'Mata Pelajaran')}
                ${statCard('📅', years.data?.length || 0, 'Tahun Akademik')}
            </div>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:20px;">
                <div class="data-card">
                    <div class="data-card-header"><h3>Kelas</h3><button class="btn btn-gold btn-sm" onclick="window._addClass()">+ Tambah</button></div>
                    <table class="data-table"><thead><tr><th>Nama</th><th>Level</th><th>Kapasitas</th><th>Wali Kelas</th></tr></thead>
                    <tbody>${(classes.data || []).map(c => `<tr><td>${esc(c.name)}</td><td>${esc(c.gradeLevel || '-')}</td><td>${c.capacity}</td><td>${esc(c.teacherName || '-')}</td></tr>`).join('') || '<tr><td colspan="4"><div class="empty-state"><p>Belum ada kelas</p></div></td></tr>'}</tbody></table>
                </div>
                <div class="data-card">
                    <div class="data-card-header"><h3>Mata Pelajaran</h3><button class="btn btn-gold btn-sm" onclick="window._addSubject()">+ Tambah</button></div>
                    <table class="data-table"><thead><tr><th>Kode</th><th>Nama</th><th>SKS</th></tr></thead>
                    <tbody>${(subjects.data || []).map(s => `<tr><td>${esc(s.code)}</td><td>${esc(s.name)}</td><td>${s.credits}</td></tr>`).join('') || '<tr><td colspan="3"><div class="empty-state"><p>Belum ada mata pelajaran</p></div></td></tr>'}</tbody></table>
                </div>
            </div>`;
    }

    window._addClass = () => showModal('Tambah Kelas', `
        <div class="form-group"><label>Nama Kelas *</label><input name="name" required></div>
        <div class="form-row"><div class="form-group"><label>Tingkat</label><input name="gradeLevel"></div><div class="form-group"><label>Kapasitas</label><input type="number" name="capacity" value="30"></div></div>`,
        async (data) => { const r = await api('POST', '/academic/classes', data); if (r.success) { closeModal(); loadAcademic(); toast(r.message, 'success'); } else toast(r.message, 'error'); });

    window._addSubject = () => showModal('Tambah Mata Pelajaran', `
        <div class="form-row"><div class="form-group"><label>Kode *</label><input name="code" required></div><div class="form-group"><label>Nama *</label><input name="name" required></div></div>
        <div class="form-row"><div class="form-group"><label>SKS</label><input type="number" name="credits" value="0"></div></div>
        <div class="form-group"><label>Deskripsi</label><textarea name="description"></textarea></div>`,
        async (data) => { const r = await api('POST', '/academic/subjects', data); if (r.success) { closeModal(); loadAcademic(); toast(r.message, 'success'); } else toast(r.message, 'error'); });

    // ============ FINANCE ============
    async function loadFinance() {
        const [invoices, summary] = await Promise.all([api('GET', '/finance/invoices'), api('GET', '/finance/summary')]);
        const s = summary.data || {};
        document.getElementById('content').innerHTML = `
            <div class="content-header"><div><h2>Keuangan</h2><p>Kelola invoice dan pembayaran</p></div></div>
            <div class="stats-grid">
                ${statCard('💰', formatRp(s.totalPaid || 0), 'Total Terbayar')}
                ${statCard('⏳', formatRp(s.totalUnpaid || 0), 'Belum Terbayar')}
            </div>
            <div class="data-card">
                <div class="data-card-header"><h3>Invoice Terbaru</h3></div>
                <table class="data-table"><thead><tr><th>No. Invoice</th><th>Siswa</th><th>Biaya</th><th>Jumlah</th><th>Jatuh Tempo</th><th>Status</th></tr></thead>
                <tbody>${(invoices.data || []).map(i => `<tr><td>${esc(i.invoiceNumber)}</td><td>${esc(i.studentName)}</td><td>${esc(i.feeName)}</td><td>${formatRp(i.amount)}</td><td>${esc(i.dueDate || '-')}</td><td><span class="badge ${i.status === 'paid' ? 'badge-success' : 'badge-warning'}">${esc(i.status)}</span></td></tr>`).join('') || '<tr><td colspan="6"><div class="empty-state"><div class="empty-icon">💰</div><p>Belum ada invoice</p></div></td></tr>'}</tbody></table>
            </div>`;
    }

    // ============ ATTENDANCE ============
    async function loadAttendance() {
        const res = await api('GET', '/attendance');
        document.getElementById('content').innerHTML = `
            <div class="content-header"><div><h2>Absensi</h2><p>Rekam kehadiran siswa</p></div></div>
            <div class="data-card">
                <div class="data-card-header"><h3>Absensi Hari Ini</h3></div>
                <table class="data-table"><thead><tr><th>NIS</th><th>Nama</th><th>Tanggal</th><th>Status</th><th>Keterangan</th></tr></thead>
                <tbody>${(res.data || []).length ? res.data.map(a => `<tr><td>${esc(a.nis)}</td><td>${esc(a.studentName)}</td><td>${esc(a.date)}</td><td><span class="badge ${a.status === 'hadir' ? 'badge-success' : a.status === 'izin' ? 'badge-info' : 'badge-danger'}">${esc(a.status)}</span></td><td>${esc(a.remarks || '-')}</td></tr>`).join('') : '<tr><td colspan="5"><div class="empty-state"><div class="empty-icon">📋</div><p>Belum ada data absensi hari ini</p></div></td></tr>'}</tbody></table>
            </div>`;
    }

    // ============ MODAL ============
    function showModal(title, formHtml, onSubmit) {
        const overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'modalOverlay';
        overlay.innerHTML = `
            <div class="modal">
                <h3>${title}</h3>
                <form id="modalForm">${formHtml}
                    <div class="modal-actions">
                        <button type="button" class="btn btn-outline" onclick="window._closeModal()">Batal</button>
                        <button type="submit" class="btn btn-gold">Simpan</button>
                    </div>
                </form>
            </div>`;
        document.body.appendChild(overlay);
        overlay.addEventListener('click', e => { if (e.target === overlay) closeModal(); });
        document.getElementById('modalForm').addEventListener('submit', e => {
            e.preventDefault();
            const formData = {};
            new FormData(e.target).forEach((v, k) => { formData[k] = v; });
            onSubmit(formData);
        });
    }

    function closeModal() {
        const el = document.getElementById('modalOverlay');
        if (el) el.remove();
    }
    window._closeModal = closeModal;

    // ============ UTILITIES ============
    async function api(method, path, body) {
        const opts = { method, headers: { 'Content-Type': 'application/json' }, credentials: 'same-origin' };
        if (body) opts.body = JSON.stringify(body);
        if (csrfToken && method !== 'GET') opts.headers['X-CSRF-Token'] = csrfToken;
        try {
            const res = await fetch(API + path, opts);
            if (res.status === 401) { showLogin(); return { success: false, message: 'Sesi berakhir' }; }
            return await res.json();
        } catch (err) {
            return { success: false, message: 'Koneksi gagal' };
        }
    }

    function toast(message, type = 'success') {
        const el = document.getElementById('toast');
        el.textContent = message;
        el.className = 'toast show ' + type;
        setTimeout(() => { el.className = 'toast'; }, 3000);
    }

    function esc(s) {
        if (s === null || s === undefined) return '';
        return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    function formatRp(n) {
        return 'Rp ' + Number(n).toLocaleString('id-ID');
    }

    function pagination(page, totalPages, fn) {
        if (totalPages <= 1) return '';
        return `<div class="pagination">
            <button onclick="${fn}(${page - 1})" ${page <= 1 ? 'disabled' : ''}>&laquo; Prev</button>
            <span>Halaman ${page} dari ${totalPages}</span>
            <button onclick="${fn}(${page + 1})" ${page >= totalPages ? 'disabled' : ''}>Next &raquo;</button>
        </div>`;
    }

    function debounce(fn, ms) {
        let t;
        return function (...args) { clearTimeout(t); t = setTimeout(() => fn.apply(this, args), ms); };
    }
})();
