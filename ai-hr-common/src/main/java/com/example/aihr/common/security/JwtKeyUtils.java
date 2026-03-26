package com.example.aihr.common.security;

import java.util.regex.Pattern;

public class JwtKeyUtils {

    private static final Pattern KEY_STRENGTH_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{32,}$"
    );

    private JwtKeyUtils() {}

    public static String generateKey(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@$!%*?&";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    public static boolean isStrongKey(String key) {
        return KEY_STRENGTH_PATTERN.matcher(key).matches();
    }

    public static String validateKeyStrength(String key) {
        if (key == null || key.isEmpty()) {
            return "JWT密钥不能为空";
        }
        if (key.length() < 32) {
            return "JWT密钥长度至少为32个字符";
        }
        if (!isStrongKey(key)) {
            return "JWT密钥必须包含大小写字母、数字和特殊字符";
        }
        return null;
    }
}
