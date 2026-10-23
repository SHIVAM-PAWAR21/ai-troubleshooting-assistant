package org.assignment.troubleshooter.retrieval;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VectorMathTest {

    @Test
    void identicalVectorsScoreOne() {
        assertEquals(1.0, VectorMath.cosine(new double[]{1, 2, 3}, new double[]{1, 2, 3}), 1e-9);
    }

    @Test
    void sameDirectionDifferentLengthScoresOne() {
        assertEquals(1.0, VectorMath.cosine(new double[]{1, 2, 3}, new double[]{2, 4, 6}), 1e-9);
    }

    @Test
    void perpendicularVectorsScoreZero() {
        assertEquals(0.0, VectorMath.cosine(new double[]{1, 0}, new double[]{0, 1}), 1e-9);
    }

    @Test
    void oppositeVectorsScoreMinusOne() {
        assertEquals(-1.0, VectorMath.cosine(new double[]{1, 2}, new double[]{-1, -2}), 1e-9);
    }

    @Test
    void zeroVectorScoresZero() {
        assertEquals(0.0, VectorMath.cosine(new double[]{0, 0, 0}, new double[]{1, 2, 3}), 1e-9);
    }

    @Test
    void differentSizesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> VectorMath.cosine(new double[]{1, 2}, new double[]{1, 2, 3}));
    }
}
