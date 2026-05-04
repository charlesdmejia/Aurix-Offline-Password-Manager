package core;
 
import model.PasswordEntry;
import model.Vault;
import model.VaultMeta;
 
import javax.crypto.SecretKey;
import java.io.*;
import java.util.Arrays;
 
public class VaultRepo {
 
    private static final String VAULT_FILE = "vault.dat";
    private static final String META_FILE  = "vault.meta"; 
 
    private SecretKey aesKey;
 
    public VaultRepo(SecretKey key) {
        this.aesKey = key;
    }
    public void setKey(SecretKey key) {
        this.aesKey = key;
    }
 
    // =========================
    // SAVE VAULT
    // =========================
    public void saveVault(Vault vault) throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
 
        try (ObjectOutputStream objectStream =
                new ObjectOutputStream(byteStream)) {
            objectStream.writeObject(vault);
        }
 
        byte[] vaultBytes      = byteStream.toByteArray();
        byte[] iv              = AesGcmCipher.generateIV();
        byte[] encryptedVault  = AesGcmCipher.encrypt(vaultBytes, aesKey, iv);
        byte[] hmac            = IntegrityVerifier.generateHMAC(
                                     encryptedVault, aesKey.getEncoded());
 
        try (FileOutputStream fos = new FileOutputStream(VAULT_FILE)) {
            fos.write(iv);
            fos.write(encryptedVault);
            fos.write(hmac);
        }
    }
 
    // =========================
    // LOAD VAULT
    // =========================
    public Vault loadVault() throws Exception {
        File file = new File(VAULT_FILE);
        if (!file.exists()) {
            return new Vault();
        }
 
        byte[] fileData;
        try (FileInputStream fis = new FileInputStream(file)) {
            fileData = fis.readAllBytes();
        }
 
        byte[] iv = Arrays.copyOfRange(fileData, 0, VaultConfig.IV_LENGTH);
        byte[] encryptedVault = Arrays.copyOfRange(
            fileData,
            VaultConfig.IV_LENGTH,
            fileData.length - 32
        );
        byte[] storedHmac = Arrays.copyOfRange(
            fileData,
            fileData.length - 32,
            fileData.length
        );
 
        boolean valid = IntegrityVerifier.verifyHMAC(
            encryptedVault, aesKey.getEncoded(), storedHmac);
        if (!valid) {
            throw new SecurityException("Vault file has been modified!");
        }
 
        byte[] decrypted = AesGcmCipher.decrypt(encryptedVault, aesKey, iv);
 
        try (ObjectInputStream objectStream = new ObjectInputStream(
                new ByteArrayInputStream(decrypted))) {
            return (Vault) objectStream.readObject();
        }
    }
 
    // =========================
    // LOAD OR CREATE META
    // =========================
    public VaultMeta loadOrCreateMeta() throws Exception {
        File file = new File(META_FILE);
 
        if (!file.exists()) {
            // First run — generate a new salt and persist it
            byte[] salt    = KeyDerivation.generateSalt();
            VaultMeta meta = new VaultMeta(salt);
            saveMeta(meta);
            return meta;
        }
 
        // Subsequent runs — load the existing salt
        try (FileInputStream fis   = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            return (VaultMeta) ois.readObject();
        }
    }
 
    // =========================
    // SAVE META
    // =========================
    private void saveMeta(VaultMeta meta) throws Exception {
        try (FileOutputStream fos   = new FileOutputStream(META_FILE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(meta);
        }
    }
 
    // =========================
    // ADD PASSWORD ENTRY
    // =========================
    public void addEntry(Vault vault, PasswordEntry entry) {
        vault.getEntries().add(entry);
    }
 
    // =========================
    // DELETE PASSWORD ENTRY
    // =========================
    public void deleteEntry(Vault vault, int index) {
        if (index >= 0 && index < vault.getEntries().size()) {
            vault.getEntries().remove(index);
        }
    }
 
    // =========================
    // UPDATE PASSWORD ENTRY
    // =========================
    public void updateEntry(Vault vault, int index, PasswordEntry entry) {
        if (index >= 0 && index < vault.getEntries().size()) {
            vault.getEntries().set(index, entry);
        }
    }
 
    // =========================
    // BACKUP VAULT
    // =========================
    // Also backs up vault.meta — without it the vault is unreadable
    public void backupVault(String backupFile) throws Exception {
        copyFile(VAULT_FILE, backupFile);
        copyFile(META_FILE,  backupFile + ".meta");
    }
 
    // =========================
    // RESTORE VAULT
    // =========================
    public void restoreVault(String backupFile) throws Exception {
        copyFile(backupFile,           VAULT_FILE);
        copyFile(backupFile + ".meta", META_FILE);
    }
 
    // =========================
    // ADD ENTRY AND HISTORY
    // =========================
    public void addEntryAndHistory(Vault vault, PasswordEntry entry,
            byte[] hash) throws Exception {
        vault.getEntries().add(entry);
        vault.addHash(hash);
        saveVault(vault);
    }
 
    // =========================
    // PRIVATE HELPER
    // =========================
    private void copyFile(String src, String dst) throws Exception {
        try (FileInputStream  fis = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dst)) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
    }
}