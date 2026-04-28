package id.kamsib.antaboga.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Password hashing utility using BCrypt.
 */
public class PasswordHasher {
    private static final int ROUNDS = 12;

    public static String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(ROUNDS));
    }

    public static boolean verify(String password, String hash) {
        try {
            return BCrypt.checkpw(password, hash);
        } catch (Exception e) {
            return false;
        }
    }
}
