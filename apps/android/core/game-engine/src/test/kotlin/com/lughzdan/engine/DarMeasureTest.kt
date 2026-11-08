package com.lughzdan.engine
import org.junit.Assert.*
import org.junit.Test
class DarMeasureTest {
    @Test fun boundsAndTolerance() {
        assertEquals(27.47477,DarMeasure.shadow(10.0,20),0.0001)
        assertEquals(3.63970,DarMeasure.shadow(10.0,70),0.0001)
        assertTrue(DarMeasure.matches(20.49,20.0,45))
        assertFalse(DarMeasure.matches(20.51,20.0,45))
        assertTrue(runCatching { DarMeasure.shadow(10.0,19) }.isFailure)
    }
}
