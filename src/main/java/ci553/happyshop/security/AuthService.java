package ci553.happyshop.security;

import ci553.happyshop.storageAccess.DatabaseRW;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;

public class AuthService {
    private final DatabaseRW db;

    public AuthService(DatabaseRW db) {
        this.db = db;
    }

    public boolean signup(String username, String plainPassword, String role) throws SQLException {
        String uname = username.trim().toLowerCase();
        if (uname.length() < 3 || plainPassword.length() < 6) {
            throw new IllegalArgumentException("username/password too short");
        }
        if (db.findUserByUsername(uname) != null) {
            return false; // user exists
        }
        String hashed = hashPassword(plainPassword);
        return db.createUser(uname, hashed, role);
    }

    public User login(String username, String plainPassword) throws SQLException {
        String uname = username.trim().toLowerCase();
        User u = db.findUserByUsername(uname);
        if (u == null) return null;
        if (verifyPassword(plainPassword, u.getPasswordHash())) return u;
        return null;
    }

    public boolean changePassword(String username, String newPlainPassword) throws SQLException {
        String newHash = hashPassword(newPlainPassword);
        return db.updateUserPassword(username.trim().toLowerCase(), newHash);
    }

    private String hashPassword(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(12));
    }

    private boolean verifyPassword(String plain, String hash) {
        try {
            return BCrypt.checkpw(plain, hash);
        } catch (Exception e) {
            return false;
        }
    }
}
