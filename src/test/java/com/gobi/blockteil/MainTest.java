package com.gobi.blockteil;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MainTest {

    @Test
    void testAdd() {
        Main main = new Main();
        assertEquals(5, main.add(2, 3));
    }
}
