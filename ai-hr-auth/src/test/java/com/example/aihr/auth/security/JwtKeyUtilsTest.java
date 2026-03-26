package com.example.aihr.auth.security;

import static org.junit.jupiter.api.Assertions.*;

import com.example.aihr.common.security.JwtKeyUtils;
import org.junit.jupiter.api.Test;

class JwtKeyUtilsTest {

    @Test
    void testGenerateKey() {
        String key = JwtKeyUtils.generateKey(32);
        assertNotNull(key);
        assertEquals(32, key.length());
    }

    @Test
    void testIsStrongKey() {
        // 强密钥：长度足够，包含大小写字母、数字和特殊字符
        assertTrue(JwtKeyUtils.isStrongKey("MyStrongKey123!@&MyStrongKey123!@&"));
        
        // 弱密钥：长度不足
        assertFalse(JwtKeyUtils.isStrongKey("Short123!"));
        
        // 弱密钥：缺少大写字母
        assertFalse(JwtKeyUtils.isStrongKey("mystrongkey123!@&mystrongkey123!@&"));
        
        // 弱密钥：缺少小写字母
        assertFalse(JwtKeyUtils.isStrongKey("MYSTRONGKEY123!@&MYSTRONGKEY123!@&"));
        
        // 弱密钥：缺少数字
        assertFalse(JwtKeyUtils.isStrongKey("MyStrongKey!@&MyStrongKey!@&"));
        
        // 弱密钥：缺少特殊字符
        assertFalse(JwtKeyUtils.isStrongKey("MyStrongKey123MyStrongKey123"));
    }

    @Test
    void testValidateKeyStrength() {
        // 强密钥
        assertNull(JwtKeyUtils.validateKeyStrength("MyStrongKey123!@&MyStrongKey123!@&"));
        
        // 空密钥
        assertEquals("JWT密钥不能为空", JwtKeyUtils.validateKeyStrength(null));
        
        // 长度不足
        assertEquals("JWT密钥长度至少为32个字符", JwtKeyUtils.validateKeyStrength("Short123!"));
        
        // 缺少大写字母
        assertEquals("JWT密钥必须包含大小写字母、数字和特殊字符", 
            JwtKeyUtils.validateKeyStrength("mystrongkey123!@&mystrongkey123!@&"));
        
        // 缺少小写字母
        assertEquals("JWT密钥必须包含大小写字母、数字和特殊字符", 
            JwtKeyUtils.validateKeyStrength("MYSTRONGKEY123!@&MYSTRONGKEY123!@&"));
        
        // 缺少数字
        assertEquals("JWT密钥必须包含大小写字母、数字和特殊字符", 
            JwtKeyUtils.validateKeyStrength("MyStrongKey!@&MyStrongKey!@&ABCD"));
        
        // 缺少特殊字符
        assertEquals("JWT密钥必须包含大小写字母、数字和特殊字符", 
            JwtKeyUtils.validateKeyStrength("MyStrongKey123MyStrongKey123abcd"));
    }
}
