package net.uattest.sdk;

public class HexUtil {
    public static byte[] decode(String hex) {
        if (hex == null) return new byte[0];
        String cleaned = hex.trim();
        int len = cleaned.length();
        if (len % 2 != 0) {
            cleaned = "0" + cleaned;
            len += 1;
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(cleaned.charAt(i), 16) << 4)
                    + Character.digit(cleaned.charAt(i + 1), 16));
        }
        return data;
    }
}
