package core;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;

public class AesGcmCipher{
	
	private static SecureRandom random = new SecureRandom();
	private static final int GCM_TAG_LENGTH = 128;
	
	public static SecretKey generateAESKey() throws Exception{
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(VaultConfig.KEY_LENGTH);;
		return keyGen.generateKey();
	}
	
	public static byte[] encrypt(byte[] plaintext, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        return cipher.doFinal(plaintext);
    }
	
    public static byte[] decrypt(byte[] ciphertext, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        return cipher.doFinal(ciphertext);
    }
 
    public static byte[] generateIV() {
        byte[] iv = new byte[VaultConfig.IV_LENGTH];
        random.nextBytes(iv);
        return iv;
    }
}


// GCM_TAG_LENGTH is set to 128 kasi 'yun yung recommended length