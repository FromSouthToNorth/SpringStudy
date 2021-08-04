package org.springframework.pattern;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternTest {

    @Test
    void patternTest() {
        Pattern pattern = Pattern.compile("[abc]");
        Matcher matcher = pattern.matcher("123");
        System.out.println(matcher);
    }

}
