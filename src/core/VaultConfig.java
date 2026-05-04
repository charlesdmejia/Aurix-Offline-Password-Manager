package core;
 
public class VaultConfig {
 
    public static final int PBKDF2_ITERATIONS = 100_000;
    public static final int KEY_LENGTH        = 256;
    public static final int SALT_LENGTH       = 16;
    public static final int IV_LENGTH         = 12;
    public static final int MAX_LOGIN_ATTEMPTS = 5;
    public static final int LOCKOUT_DURATION_MS = 15 * 60 * 1000; 
    public static final byte[] PEPPER = loadPepper();
 
    private static byte[] loadPepper() {
        String env = System.getenv("VAULT_PEPPER");
        if (env != null && !env.isEmpty()) {
            return env.getBytes();
        }
        return "MySecretPepper".getBytes();
    }
}