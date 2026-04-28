package id.kamsib.antaboga.desktop;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Antaboga Desktop Application — launched via Java Web Start (JNLP).
 * Connects to Antaboga ERP server API for real-time reports.
 */
public class AntabogaDesktop extends JFrame {

    private static final String APP_TITLE = "Antaboga ERP — Desktop Report";
    private static final Color BG_DARK = new Color(10, 10, 26);
    private static final Color BG_CARD = new Color(22, 22, 58);
    private static final Color GOLD = new Color(201, 162, 39);
    private static final Color GOLD_LIGHT = new Color(247, 208, 107);
    private static final Color TEXT = new Color(224, 224, 238);
    private static final Color DIM = new Color(106, 106, 138);
    private static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 22);
    private static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font FONT_LABEL = new Font("SansSerif", Font.BOLD, 11);
    private static final Font FONT_MONO = new Font("Monospaced", Font.PLAIN, 13);

    private String serverBase = "http://127.0.0.1:1337";
    private String sessionCookie = null;
    private String currentUser = "Guest";
    private JTextArea reportArea;
    private JLabel statusLabel;

    public AntabogaDesktop() {
        setTitle(APP_TITLE);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(950, 680);
        setMinimumSize(new Dimension(750, 500));
        setLocationRelativeTo(null);

        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(BG_DARK);
        mainPanel.add(createHeader(), BorderLayout.NORTH);
        mainPanel.add(createContent(), BorderLayout.CENTER);
        mainPanel.add(createFooter(), BorderLayout.SOUTH);
        setContentPane(mainPanel);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(201, 162, 39, 30)),
            BorderFactory.createEmptyBorder(16, 24, 16, 24)));

        JLabel logo = new JLabel("\uD83D\uDC09 Antaboga Desktop Report");
        logo.setFont(FONT_TITLE);
        logo.setForeground(GOLD);
        header.add(logo, BorderLayout.WEST);

        statusLabel = new JLabel("Belum login");
        statusLabel.setFont(FONT_BODY);
        statusLabel.setForeground(DIM);
        header.add(statusLabel, BorderLayout.EAST);

        return header;
    }

    private JPanel createContent() {
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        toolbar.setOpaque(false);
        String[] reports = {"Ringkasan Umum", "Laporan Siswa", "Laporan Guru", "Laporan Keuangan"};
        ButtonGroup bg = new ButtonGroup();
        for (String r : reports) {
            JToggleButton btn = new JToggleButton(r);
            btn.setFont(FONT_LABEL);
            btn.setForeground(DIM);
            btn.setBackground(BG_CARD);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(201, 162, 39, 30)),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));
            btn.addActionListener(e -> loadReport(r));
            bg.add(btn);
            toolbar.add(btn);
            if (r.equals("Ringkasan Umum")) btn.setSelected(true);
        }
        content.add(toolbar, BorderLayout.NORTH);

        // Report area
        JPanel reportPanel = new JPanel(new BorderLayout());
        reportPanel.setBackground(BG_CARD);
        reportPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(201, 162, 39, 30)),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)));

        reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setFont(FONT_MONO);
        reportArea.setBackground(BG_CARD);
        reportArea.setForeground(TEXT);
        reportArea.setCaretColor(GOLD);
        reportArea.setBorder(null);
        reportArea.setText("  Silakan login terlebih dahulu...");

        JScrollPane scroll = new JScrollPane(reportArea);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_CARD);
        reportPanel.add(scroll, BorderLayout.CENTER);
        content.add(reportPanel, BorderLayout.CENTER);

        // Buttons (Login on left, others on right)
        JPanel btns = new JPanel(new BorderLayout());
        btns.setOpaque(false);

        JButton loginBtn = createButton("\uD83D\uDD11 Login", new Color(46, 204, 113), BG_DARK);
        loginBtn.addActionListener(e -> showLoginDialog());
        btns.add(loginBtn, BorderLayout.WEST);

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBtns.setOpaque(false);
        JButton refreshBtn = createButton("\uD83D\uDD04 Refresh", BG_CARD, TEXT);
        refreshBtn.addActionListener(e -> loadReport(null));
        rightBtns.add(refreshBtn);
        JButton exportBtn = createButton("\uD83D\uDCBE Export ke TXT", GOLD, BG_DARK);
        exportBtn.addActionListener(e -> exportReport());
        rightBtns.add(exportBtn);
        btns.add(rightBtns, BorderLayout.EAST);

        content.add(btns, BorderLayout.SOUTH);
        return content;
    }

    private JButton createButton(String text, Color bgColor, Color fgColor) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_LABEL);
        btn.setForeground(fgColor);
        btn.setBackground(bgColor);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
        return btn;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel();
        footer.setBackground(BG_CARD);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(201, 162, 39, 30)),
            BorderFactory.createEmptyBorder(8, 24, 8, 24)));
        JLabel label = new JLabel("\uD83D\uDC09 Antaboga ERP v1.2.0 — Desktop Report | Powered by Kamsib");
        label.setFont(new Font("SansSerif", Font.PLAIN, 11));
        label.setForeground(DIM);
        footer.add(label);
        return footer;
    }

    // ====== LOGIN ======

    private void showLoginDialog() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 8, 8));
        JTextField serverField = new JTextField(serverBase);
        JTextField userField = new JTextField("admin");
        JPasswordField passField = new JPasswordField();
        panel.add(new JLabel("Server:")); panel.add(serverField);
        panel.add(new JLabel("Username:")); panel.add(userField);
        panel.add(new JLabel("Password:")); panel.add(passField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Login ke Antaboga ERP",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            serverBase = serverField.getText().trim();
            String user = userField.getText().trim();
            String pass = new String(passField.getPassword());
            doLogin(user, pass);
        }
    }

    private void doLogin(String username, String password) {
        try {
            URL url = new URL(serverBase + "/api/auth/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);

            // Send login JSON
            String json = "{\"username\":\"" + escJson(username) + "\",\"password\":\"" + escJson(password) + "\"}";
            byte[] payload = json.getBytes("UTF-8");
            conn.setFixedLengthStreamingMode(payload.length);
            OutputStream out = conn.getOutputStream();
            out.write(payload);
            out.flush();
            out.close();

            int code = conn.getResponseCode();

            if (code == 200) {
                // Read response body
                String respBody = readStream(conn.getInputStream());

                // PRIMARY: Extract sessionId from JSON response body
                String sid = extractJson(respBody, "sessionId");
                if (sid != null && !sid.isEmpty()) {
                    sessionCookie = "ANTABOGA_SESSION=" + sid;
                }

                // FALLBACK: Try Set-Cookie header if body didn't have sessionId
                if (sessionCookie == null) {
                    String cookies = conn.getHeaderField("Set-Cookie");
                    if (cookies != null) {
                        for (String part : cookies.split(";")) {
                            part = part.trim();
                            if (part.startsWith("ANTABOGA_SESSION=")) {
                                sessionCookie = part;
                                break;
                            }
                        }
                    }
                }

                conn.disconnect();

                if (sessionCookie == null || sessionCookie.equals("ANTABOGA_SESSION=")) {
                    sessionCookie = null;
                    JOptionPane.showMessageDialog(this,
                        "Login berhasil tapi session tidak diterima.\nResponse: " + respBody,
                        APP_TITLE, JOptionPane.WARNING_MESSAGE);
                    return;
                }

                currentUser = username;
                statusLabel.setText("\u2705 " + currentUser + " @ " + serverBase);
                statusLabel.setForeground(new Color(46, 204, 113));
                loadReport("Ringkasan Umum");
            } else {
                // Read error response to show actual server message
                String errBody = readStream(conn.getErrorStream());
                conn.disconnect();
                String errMsg = extractJson(errBody, "message");
                if (errMsg.isEmpty()) errMsg = "HTTP " + code;

                JOptionPane.showMessageDialog(this,
                    "Login gagal (" + code + "):\n" + errMsg,
                    APP_TITLE, JOptionPane.ERROR_MESSAGE);
            }
        } catch (java.net.ConnectException e) {
            JOptionPane.showMessageDialog(this,
                "Tidak dapat terhubung ke server.\n"
                + "Pastikan Antaboga ERP berjalan di: " + serverBase + "\n\n"
                + "Error: " + e.getMessage(),
                APP_TITLE, JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Terjadi kesalahan saat login:\n" + e.getClass().getSimpleName() + ": " + e.getMessage(),
                APP_TITLE, JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Read an InputStream to String safely */
    private String readStream(InputStream is) {
        if (is == null) return "";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // ====== API CLIENT ======

    private String apiGet(String path) {
        try {
            URL url = new URL(serverBase + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if (sessionCookie != null) {
                conn.setRequestProperty("Cookie", sessionCookie);
            }

            if (conn.getResponseCode() != 200) {
                return "{\"error\":\"HTTP " + conn.getResponseCode() + "\"}";
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            conn.disconnect();
            return sb.toString();
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    // ====== REPORT GENERATION ======

    private String lastReport = "Ringkasan Umum";

    private void loadReport(String reportName) {
        if (reportName != null) lastReport = reportName;
        else reportName = lastReport;

        if (sessionCookie == null) {
            reportArea.setText("  ⚠ Belum login. Klik tombol 'Login' untuk memulai.");
            return;
        }

        reportArea.setText("  ⏳ Memuat data dari server...");
        final String rn = reportName;

        new Thread(() -> {
            String content;
            switch (rn) {
                case "Laporan Siswa": content = generateStudentReport(); break;
                case "Laporan Guru": content = generateTeacherReport(); break;
                case "Laporan Keuangan": content = generateFinanceReport(); break;
                default: content = generateSummaryReport();
            }
            SwingUtilities.invokeLater(() -> {
                reportArea.setText(content);
                reportArea.setCaretPosition(0);
            });
        }).start();
    }

    private String generateSummaryReport() {
        String dashJson = apiGet("/api/dashboard");
        StringBuilder sb = new StringBuilder();
        sb.append(header("RINGKASAN UMUM"));
        sb.append(meta());
        sb.append("\n");

        // Parse dashboard JSON manually (no Gson in desktop context)
        String totalStudents = extractJson(dashJson, "totalStudents");
        String totalTeachers = extractJson(dashJson, "totalTeachers");
        String totalClasses = extractJson(dashJson, "totalClasses");
        String totalSubjects = extractJson(dashJson, "totalSubjects");
        String unpaidInvoices = extractJson(dashJson, "unpaidInvoices");

        sb.append("  ┌────────────────────────┬──────────────┐\n");
        sb.append("  │ Metrik                 │ Nilai        │\n");
        sb.append("  ├────────────────────────┼──────────────┤\n");
        sb.append(tableRow("Total Siswa", totalStudents));
        sb.append(tableRow("Total Guru", totalTeachers));
        sb.append(tableRow("Total Kelas", totalClasses));
        sb.append(tableRow("Mata Pelajaran", totalSubjects));
        sb.append(tableRow("Invoice Belum Bayar", unpaidInvoices));
        sb.append("  └────────────────────────┴──────────────┘\n");

        sb.append("\n  Pilih tab di atas untuk laporan detail.\n");
        sb.append("\n  \uD83D\uDC09 Antaboga — Menjaga pondasi pendidikan Indonesia\n");
        return sb.toString();
    }

    private String generateStudentReport() {
        String json = apiGet("/api/students");
        StringBuilder sb = new StringBuilder();
        sb.append(header("LAPORAN DATA SISWA"));
        sb.append(meta());
        sb.append("\n");

        // Parse student array
        String[] students = extractArray(json);
        if (students.length == 0) {
            sb.append("  Belum ada data siswa.\n");
            return sb.toString();
        }

        sb.append("  Total: ").append(students.length).append(" siswa\n\n");
        sb.append("  ┌─────┬──────────────┬──────────────────────────┬────────┬──────────────┬──────────┐\n");
        sb.append("  │ No. │ NIS          │ Nama Lengkap             │ Gender │ Telepon      │ Status   │\n");
        sb.append("  ├─────┼──────────────┼──────────────────────────┼────────┼──────────────┼──────────┤\n");

        int no = 1;
        for (String s : students) {
            String nis = extractJson(s, "nis");
            String name = extractJson(s, "fullName");
            if (name.isEmpty()) name = extractJson(s, "full_name");
            String gender = extractJson(s, "gender");
            String phone = extractJson(s, "phone");
            String status = extractJson(s, "status");

            sb.append(String.format("  │ %3d │ %-12s │ %-24s │ %-6s │ %-12s │ %-8s │\n",
                no++, trunc(nis, 12), trunc(name, 24), trunc(gender, 6), trunc(phone, 12), trunc(status, 8)));
        }
        sb.append("  └─────┴──────────────┴──────────────────────────┴────────┴──────────────┴──────────┘\n");
        sb.append("\n  \uD83D\uDC09 Antaboga ERP v1.2.0\n");
        return sb.toString();
    }

    private String generateTeacherReport() {
        String json = apiGet("/api/teachers");
        StringBuilder sb = new StringBuilder();
        sb.append(header("LAPORAN DATA GURU"));
        sb.append(meta());
        sb.append("\n");

        String[] teachers = extractArray(json);
        if (teachers.length == 0) {
            sb.append("  Belum ada data guru.\n");
            return sb.toString();
        }

        sb.append("  Total: ").append(teachers.length).append(" guru\n\n");
        sb.append("  ┌─────┬──────────────┬──────────────────────────┬──────────────────────┬──────────┐\n");
        sb.append("  │ No. │ NIP          │ Nama Lengkap             │ Spesialisasi         │ Status   │\n");
        sb.append("  ├─────┼──────────────┼──────────────────────────┼──────────────────────┼──────────┤\n");

        int no = 1;
        for (String t : teachers) {
            String nip = extractJson(t, "nip");
            String name = extractJson(t, "fullName");
            if (name.isEmpty()) name = extractJson(t, "full_name");
            String spec = extractJson(t, "subjectSpecialization");
            if (spec.isEmpty()) spec = extractJson(t, "subject_specialization");
            String status = extractJson(t, "status");

            sb.append(String.format("  │ %3d │ %-12s │ %-24s │ %-20s │ %-8s │\n",
                no++, trunc(nip, 12), trunc(name, 24), trunc(spec, 20), trunc(status, 8)));
        }
        sb.append("  └─────┴──────────────┴──────────────────────────┴──────────────────────┴──────────┘\n");
        sb.append("\n  \uD83D\uDC09 Antaboga ERP v1.2.0\n");
        return sb.toString();
    }

    private String generateFinanceReport() {
        String dashJson = apiGet("/api/dashboard");
        StringBuilder sb = new StringBuilder();
        sb.append(header("LAPORAN KEUANGAN"));
        sb.append(meta());
        sb.append("\n");

        String unpaid = extractJson(dashJson, "unpaidInvoices");

        sb.append("  ┌────────────────────────────┬──────────────────┐\n");
        sb.append("  │ Keterangan                 │ Nilai            │\n");
        sb.append("  ├────────────────────────────┼──────────────────┤\n");
        sb.append(String.format("  │ %-26s │ %16s │\n", "Invoice Belum Bayar", unpaid));
        sb.append("  └────────────────────────────┴──────────────────┘\n");

        sb.append("\n  [INFO] Untuk laporan keuangan lengkap, gunakan modul\n");
        sb.append("         Keuangan di web: /OA_HTML/RF.jsp?module=finance\n");
        sb.append("\n  \uD83D\uDC09 Antaboga ERP v1.2.0\n");
        return sb.toString();
    }

    // ====== HELPERS ======

    private String header(String title) {
        return "\n  \u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557\n"
             + "  \u2551  " + String.format("%-57s", title) + "\u2551\n"
             + "  \u255a\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255d\n";
    }

    private String meta() {
        return "\n  Tanggal Cetak : " + new SimpleDateFormat("dd MMMM yyyy, HH:mm:ss").format(new Date())
             + "\n  User          : " + currentUser
             + "\n  Server        : " + serverBase
             + "\n  Java          : " + System.getProperty("java.version")
             + "\n  OS            : " + System.getProperty("os.name") + " " + System.getProperty("os.version")
             + "\n";
    }

    private String tableRow(String label, String value) {
        return String.format("  \u2502 %-22s \u2502 %12s \u2502\n", label, value.isEmpty() ? "0" : value);
    }

    private String trunc(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "\u2026" : s;
    }

    /** Escape special characters for JSON string values */
    private String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    /** Simple JSON value extractor — no library needed */
    private String extractJson(String json, String key) {
        if (json == null) return "";
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return "";
        int start = idx + search.length();
        // Skip whitespace
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length()) return "";

        if (json.charAt(start) == '"') {
            // String value
            int end = json.indexOf('"', start + 1);
            return end > start ? json.substring(start + 1, end) : "";
        } else {
            // Number/boolean
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            return json.substring(start, end).trim();
        }
    }

    /** Extract JSON array items (each {...} object) */
    private String[] extractArray(String json) {
        if (json == null || !json.contains("[")) return new String[0];
        int arrStart = json.indexOf('[');
        int arrEnd = json.lastIndexOf(']');
        if (arrStart < 0 || arrEnd <= arrStart) return new String[0];

        String content = json.substring(arrStart + 1, arrEnd).trim();
        if (content.isEmpty()) return new String[0];

        java.util.List<String> items = new java.util.ArrayList<>();
        int depth = 0;
        int itemStart = 0;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') { if (depth == 0) itemStart = i; depth++; }
            else if (c == '}') { depth--; if (depth == 0) items.add(content.substring(itemStart, i + 1)); }
        }
        return items.toArray(new String[0]);
    }

    // ====== EXPORT ======

    private void exportReport() {
        if (reportArea.getText().isEmpty()) return;
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("antaboga_" + lastReport.toLowerCase().replace(" ", "_")
            + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(fc.getSelectedFile(), "UTF-8")) {
                pw.print(reportArea.getText());
                JOptionPane.showMessageDialog(this,
                    "Berhasil disimpan ke:\n" + fc.getSelectedFile().getAbsolutePath(),
                    APP_TITLE, JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Gagal menyimpan: " + ex.getMessage(),
                    APP_TITLE, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ====== MAIN ======

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AntabogaDesktop app = new AntabogaDesktop();
            app.setVisible(true);
            // Auto-show login on start
            app.showLoginDialog();
        });
    }
}
