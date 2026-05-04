package core;

public class AccessLimiter {

    private int     loginAttempts = 0;
    private boolean locked        = false;
    private long    lockTimestamp = 0;

    public boolean attemptLogin() {
        if (locked) {
            long elapsed = System.currentTimeMillis() - lockTimestamp;
            if (elapsed >= VaultConfig.LOCKOUT_DURATION_MS) {
                resetAttempts();
            } else {
                return false;
            }
        }

        loginAttempts++;

        if (loginAttempts >= VaultConfig.MAX_LOGIN_ATTEMPTS) {
            locked        = true;
            lockTimestamp = System.currentTimeMillis();
            System.out.println("System locked. Max attempts reached!");
            return false;
        }

        return true;
    }

    public void resetAttempts() {
        loginAttempts = 0;
        locked        = false;
        lockTimestamp = 0;
    }

    public boolean isLocked() {
        if (locked) {
            long elapsed = System.currentTimeMillis() - lockTimestamp;
            if (elapsed >= VaultConfig.LOCKOUT_DURATION_MS) {
                resetAttempts();
                return false;
            }
        }
        return locked;
    }

    // Returns how many seconds remain in the current lockout (0 if not locked)
    public long getRemainingLockoutSeconds() {
        if (!locked) return 0;
        long elapsed   = System.currentTimeMillis() - lockTimestamp;
        long remaining = (VaultConfig.LOCKOUT_DURATION_MS - elapsed) / 1000;
        return Math.max(0, remaining);
    }

}
