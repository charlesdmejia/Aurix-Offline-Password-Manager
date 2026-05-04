package core;
 
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

public class KeyDerivation {
 
    private static final SecureRandom random = new SecureRandom();
    public static byte[] generateSalt() {
        byte[] salt = new byte[VaultConfig.SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }
 
    public static byte[] hashPassword(String password, byte[] salt)
            throws Exception {
        String combined = password + new String(VaultConfig.PEPPER);
        KeySpec spec = new PBEKeySpec(
            combined.toCharArray(),
            salt,
            VaultConfig.PBKDF2_ITERATIONS,
            VaultConfig.KEY_LENGTH
        );
        SecretKeyFactory factory =
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec).getEncoded();
    }
 
    public static SecretKey generateKey(String password, byte[] salt)
            throws Exception {
        byte[] keyBytes = hashPassword(password, salt);
        return new SecretKeySpec(keyBytes, "AES");
    }
 
    public static boolean verifyPassword(String password, byte[] salt,
            byte[] expectedHash) throws Exception {
        byte[] hash = hashPassword(password, salt);
        return Arrays.equals(hash, expectedHash);
    }
}