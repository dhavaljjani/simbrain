package org.simbrain.util.geneticalgorithms.numerical;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class DoubleGenomeTest {

    @Test
    public void phenotypeLengthMatchesChromosomeLength() {

        DoubleGenome dg = new DoubleGenome(3);
        List<Double> phenotype = dg.express();

        // Genome with length 3 chromosome should express a list of double of length 3
        assertEquals(phenotype.size(), 3);

    }
}