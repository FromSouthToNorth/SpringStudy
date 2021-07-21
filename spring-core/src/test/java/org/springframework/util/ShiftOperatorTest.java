package org.springframework.util;

import org.junit.jupiter.api.Test;

public class ShiftOperatorTest {

    @Test
    void shiftTest() {
        int number = 10;

        number = number << 10;

        System.out.println(number);

        number = (number >> 10) & 0xff;
        System.out.println("number = " + number);
    }

}
