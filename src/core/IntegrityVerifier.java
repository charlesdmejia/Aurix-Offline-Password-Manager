package core;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

public class IntegrityVerifier {

	public static byte[] generateHMAC(byte[] data, byte[] key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key, "HmacSHA256");
        mac.init(secretKey);
        return mac.doFinal(data);
    }

    public static boolean verifyHMAC(byte[] data, byte[] key, byte[] expectedHmac) throws Exception {
        byte[] actualHmac = generateHMAC(data, key);
        return Arrays.equals(actualHmac, expectedHmac);
    }
}
