package ci553.happyshop.security;

public class UserSession {
    private static UserSession instance;
    private User user;

    private UserSession() {}

    public static synchronized UserSession get() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    public void setUser(User user) { this.user = user; }
    public User getUser() { return user; }
    public void clear() { user = null; }
    public boolean isLoggedIn() { return user != null; }
}
