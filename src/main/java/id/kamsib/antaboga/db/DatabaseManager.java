package id.kamsib.antaboga.db;

import id.kamsib.antaboga.config.AppConfig;
import id.kamsib.antaboga.security.PasswordHasher;

import java.io.File;
import java.sql.*;

/**
 * Manages H2 embedded database connections and schema.
 */
public class DatabaseManager {
    private final AppConfig config;
    private Connection connection;

    public DatabaseManager(AppConfig config) {
        this.config = config;
    }

    public void initialize() throws SQLException {
        File dataDir = new File(config.getDbPath()).getParentFile();
        if (dataDir != null && !dataDir.exists()) {
            dataDir.mkdirs();
        }

        String url = "jdbc:h2:" + config.getDbPath() + ";MODE=MySQL";
        connection = DriverManager.getConnection(url, config.getDbUsername(), config.getDbPassword());

        createSchema();
        seedDefaultData();
        System.out.println("  [OK] Database initialized");
    }

    private void createSchema() throws SQLException {
        Statement s = connection.createStatement();

        s.execute("CREATE TABLE IF NOT EXISTS users ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "username VARCHAR(50) UNIQUE NOT NULL,"
            + "password_hash VARCHAR(255) NOT NULL,"
            + "full_name VARCHAR(100) NOT NULL,"
            + "email VARCHAR(100),"
            + "role VARCHAR(20) NOT NULL DEFAULT 'user',"
            + "is_active BOOLEAN DEFAULT TRUE,"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        s.execute("CREATE TABLE IF NOT EXISTS sessions ("
            + "id VARCHAR(64) PRIMARY KEY,"
            + "user_id INT NOT NULL,"
            + "csrf_token VARCHAR(64) NOT NULL,"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "expires_at TIMESTAMP NOT NULL,"
            + "FOREIGN KEY (user_id) REFERENCES users(id))");

        s.execute("CREATE TABLE IF NOT EXISTS students ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "nis VARCHAR(20) UNIQUE NOT NULL,"
            + "full_name VARCHAR(100) NOT NULL,"
            + "gender VARCHAR(10),"
            + "birth_date DATE,"
            + "birth_place VARCHAR(50),"
            + "address TEXT,"
            + "phone VARCHAR(20),"
            + "email VARCHAR(100),"
            + "parent_name VARCHAR(100),"
            + "parent_phone VARCHAR(20),"
            + "class_id INT,"
            + "enrollment_date DATE DEFAULT CURRENT_DATE,"
            + "status VARCHAR(20) DEFAULT 'active',"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        s.execute("CREATE TABLE IF NOT EXISTS teachers ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "nip VARCHAR(30) UNIQUE NOT NULL,"
            + "full_name VARCHAR(100) NOT NULL,"
            + "gender VARCHAR(10),"
            + "birth_date DATE,"
            + "address TEXT,"
            + "phone VARCHAR(20),"
            + "email VARCHAR(100),"
            + "subject_specialization VARCHAR(100),"
            + "qualification VARCHAR(100),"
            + "join_date DATE DEFAULT CURRENT_DATE,"
            + "status VARCHAR(20) DEFAULT 'active',"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        s.execute("CREATE TABLE IF NOT EXISTS academic_years ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "name VARCHAR(50) NOT NULL,"
            + "start_date DATE NOT NULL,"
            + "end_date DATE NOT NULL,"
            + "is_active BOOLEAN DEFAULT FALSE,"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        s.execute("CREATE TABLE IF NOT EXISTS classes ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "name VARCHAR(50) NOT NULL,"
            + "grade_level VARCHAR(20),"
            + "academic_year_id INT,"
            + "homeroom_teacher_id INT,"
            + "capacity INT DEFAULT 30,"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        s.execute("CREATE TABLE IF NOT EXISTS subjects ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "code VARCHAR(20) UNIQUE NOT NULL,"
            + "name VARCHAR(100) NOT NULL,"
            + "description TEXT,"
            + "credits INT DEFAULT 0,"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        s.execute("CREATE TABLE IF NOT EXISTS grades ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "student_id INT NOT NULL,"
            + "subject_id INT NOT NULL,"
            + "academic_year_id INT,"
            + "grade_type VARCHAR(30),"
            + "score DECIMAL(5,2),"
            + "remarks TEXT,"
            + "recorded_by INT,"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        s.execute("CREATE TABLE IF NOT EXISTS fee_types ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "name VARCHAR(100) NOT NULL,"
            + "amount DECIMAL(15,2) NOT NULL,"
            + "description TEXT,"
            + "academic_year_id INT,"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        s.execute("CREATE TABLE IF NOT EXISTS invoices ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "invoice_number VARCHAR(30) UNIQUE NOT NULL,"
            + "student_id INT NOT NULL,"
            + "fee_type_id INT NOT NULL,"
            + "amount DECIMAL(15,2) NOT NULL,"
            + "due_date DATE,"
            + "status VARCHAR(20) DEFAULT 'unpaid',"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        s.execute("CREATE TABLE IF NOT EXISTS payments ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "invoice_id INT NOT NULL,"
            + "amount DECIMAL(15,2) NOT NULL,"
            + "payment_method VARCHAR(30),"
            + "payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "received_by INT,"
            + "notes TEXT)");

        s.execute("CREATE TABLE IF NOT EXISTS attendance ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "student_id INT NOT NULL,"
            + "class_id INT NOT NULL,"
            + "att_date DATE NOT NULL,"
            + "status VARCHAR(20) NOT NULL,"
            + "remarks TEXT,"
            + "recorded_by INT,"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        s.execute("CREATE TABLE IF NOT EXISTS audit_log ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "user_id INT,"
            + "action VARCHAR(50) NOT NULL,"
            + "entity_type VARCHAR(50),"
            + "entity_id INT,"
            + "details TEXT,"
            + "ip_address VARCHAR(45),"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        s.close();
    }

    private void seedDefaultData() throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
            "SELECT COUNT(*) FROM users WHERE username = ?");
        ps.setString(1, "admin");
        ResultSet rs = ps.executeQuery();
        rs.next();

        if (rs.getInt(1) == 0) {
            // Generate a cryptographically secure random password
            String generatedPassword = generateSecurePassword(16);
            String hash = PasswordHasher.hash(generatedPassword);

            PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO users (username, password_hash, full_name, email, role) VALUES (?,?,?,?,?)");
            insert.setString(1, "admin");
            insert.setString(2, hash);
            insert.setString(3, "Administrator");
            insert.setString(4, "admin@antaboga.local");
            insert.setString(5, "admin");
            insert.executeUpdate();
            insert.close();

            System.out.println();
            System.out.println("  ╔══════════════════════════════════════════════╗");
            System.out.println("  ║  🔑 ADMIN ACCOUNT CREATED                   ║");
            System.out.println("  ║                                              ║");
            System.out.println("  ║  Username : admin                            ║");
            System.out.println("  ║  Password : " + padRight(generatedPassword, 33) + "║");
            System.out.println("  ║                                              ║");
            System.out.println("  ║  ⚠️  SIMPAN PASSWORD INI! Hanya ditampilkan  ║");
            System.out.println("  ║     SEKALI saat pertama kali dijalankan.     ║");
            System.out.println("  ╚══════════════════════════════════════════════╝");
            System.out.println();
        }

        rs.close();
        ps.close();
    }

    /**
     * Generate a cryptographically secure random password.
     */
    private String generateSecurePassword(int length) {
        java.security.SecureRandom random = new java.security.SecureRandom();
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghjkmnpqrstuvwxyz";
        String digits = "23456789";
        String special = "!@#$%&*";
        String all = upper + lower + digits + special;

        StringBuilder sb = new StringBuilder(length);
        // Ensure at least one of each type
        sb.append(upper.charAt(random.nextInt(upper.length())));
        sb.append(lower.charAt(random.nextInt(lower.length())));
        sb.append(digits.charAt(random.nextInt(digits.length())));
        sb.append(special.charAt(random.nextInt(special.length())));

        for (int i = 4; i < length; i++) {
            sb.append(all.charAt(random.nextInt(all.length())));
        }

        // Shuffle the password
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

    private String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("  [OK] Database closed");
            }
        } catch (SQLException e) {
            System.err.println("  [ERROR] Failed to close DB: " + e.getMessage());
        }
    }
}
