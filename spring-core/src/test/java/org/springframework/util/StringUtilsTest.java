package org.springframework.util;

import org.junit.Test;

public class StringUtilsTest {

    @Test
    public void uncapitalize() {
        String str = "Hello World";
        String updatedStr = changeFirstCharacterCase(str, true);
        System.out.println("updatedStr = " + updatedStr);
    }

    public static String changeFirstCharacterCase(String str, boolean capitalize) {
        if (str == null && str.isEmpty()) {
            return str;
        }
        char beanChar = str.charAt(0);
        char updatedChar;
        if (capitalize) {
            updatedChar = Character.toUpperCase(beanChar);
        }
        else {
            updatedChar = Character.toLowerCase(beanChar);
        }
        if (updatedChar == beanChar) {
            return str;
        }
        char[] chars = str.toCharArray();
        chars[0] = updatedChar;
        return new String(chars, 0, chars.length);
    }

}
