package core;

public class SecurityInfo {

    public static String getSecurityExplanation() {

        return
        "Security Information\n\n" +

        "AES-256 GCM Encryption:\n" +
        "Used to encrypt the vault so that stored data cannot be read without the correct key.\n\n" +

        "PBKDF2 Key Derivation:\n" +
        "Transforms the master password into a strong encryption key.\n\n" +

        "Salt and Pepper:\n" +
        "Salt prevents rainbow table attacks. Pepper adds an extra secret layer.\n\n" +

        "Zero-Knowledge Design:\n" +
        "The system does not store your master password and cannot recover it.\n\n" +

        "Offline Storage:\n" +
        "All data is stored locally to reduce exposure to online attacks.";

    }

}