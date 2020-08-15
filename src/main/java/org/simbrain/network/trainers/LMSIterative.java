/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.trainers;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.dl4j.WeightMatrix;
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule;
import org.simbrain.network.subnetworks.LMSNetwork;

import java.util.List;

/**
 * Train using least mean squares.
 * <p>
 * Clarify it is for loose neurons.  Rename: LMSIterativeLoose
 * <p>
 * Pass in input neurons, target neurons, and training dataset.  Weights can be inferred.
 *
 * @author jyoshimi
 */
public class LMSIterative extends IterableTrainer {

    /**
     * Current error.
     */
    private double rmsError;

    // TODO: Use annotations around here?
    public static double DEFAULT_LEARNING_RATE = .01;

    /**
     * Learning rate.
     */
    private double learningRate = DEFAULT_LEARNING_RATE;


    public List<Neuron> inputs;
    public List<Neuron> outputs;
    public TrainingSet ts;

    // TODO
    public LMSIterative(List<Neuron> inputs, List<Neuron> outputs, TrainingSet ts) {
        this.inputs = inputs;
        this.outputs = outputs;
        this.ts = ts;
    }

    @Override
    public double getError() {
        return rmsError;
    }


    /**
     * A standard way of randomizing networks to which LMSIterative is applied,
     * by randomizing bias on output nodes and the single layer of weights.
     */
    public void randomize() {
        for (Neuron neuron : outputs) {
            neuron.clear(); // Cleared output nodes look nicer in the GUI
            if (neuron.getUpdateRule() instanceof BiasedUpdateRule) {
                ((BiasedUpdateRule) neuron.getUpdateRule()).setBias(Math.random());
            }
        }
        // network.getWeightMatrixList().forEach(WeightMatrix::randomize);
    }

    @Override
    protected TrainingSet getTrainingSet() {
        return ts;
    }

    public double getLearningRate() {
        return learningRate;
    }

    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    @Override
    public void apply() throws DataNotInitializedException {

        rmsError = 0;

        // Set local variables
        int numRows = ts.getInputData().length;
        int numInputs = inputs.size();
        int numOutputs = outputs.size();

        // Run through training data
        for (int row = 0; row < numRows; row++) {

            // Set input layer values
            for (int i = 0; i < numInputs; i++) {
                inputs.get(i).forceSetActivation(ts.getInputData()[row][i]);
            }

            // Update output node
            Network.updateNeurons(outputs);

            // Iterate through weights and biases and update them
            for (int i = 0; i < numOutputs; i++) {

                // Get target neuron and compute error
                Neuron outputNeuron = outputs.get(i);
                double targetValue = ts.getTargetData()[row][i];
                double error = targetValue - outputNeuron.getActivation();
                rmsError += (error * error); // TODO: Validate rmse

                // Update weights
                for (Synapse synapse : outputNeuron.getFanIn()) {
                    double deltaW = (learningRate * error * synapse.getSource().getActivation());
                    synapse.setStrength(synapse.getStrength() + deltaW);
                }

                // Update bias of target neuron
                BiasedUpdateRule bias = (BiasedUpdateRule) outputNeuron.getUpdateRule();
                bias.setBias(bias.getBias() + (learningRate * error));
            }
            rmsError = rmsError / (numInputs * numOutputs);
        }
        getEvents().fireErrorUpdated();
        incrementIteration();
    }

    // TODO: Move to test class
    // /**
    // * Test method.
    // *
    // * @param args unused
    // */
    // public static void main(String[] args) {
    // test();
    // }
    //
    // /**
    // * A test with a 4-2 network and specific start weights to validate
    // against
    // * an emergent sim.
    // */
    // public static void test2() {
    //
    // double inputData[][] = { { .95, 0, 0, 0 }, { 0, .95, 0, 0 },
    // { 0, 0, .95, 0 }, { 0, 0, 0, .95 } };
    // double trainingData[][] = { { .95, 0 }, { .95, 0 }, { 0, .95 },
    // { 0, .95 } };
    //
    // // TODO: Long API! Must be shortcuts...
    //
    // // Build network
    // Network network = new Network();
    //
    // // Set up input layer
    // List<Neuron> inputLayer = new ArrayList<Neuron>();
    // for (int i = 0; i < 4; i++) {
    // Neuron neuron = new Neuron(network, new LinearRule());
    // network.addLooseNeuron(neuron);
    // inputLayer.add(neuron);
    // // System.out.println("Input " + i + " = " + neuron.getId());
    // }
    //
    // // Set up output layer
    // List<Neuron> outputLayer = new ArrayList<Neuron>();
    // for (int i = 0; i < 2; i++) {
    // LinearRule rule = new LinearRule();
    // Neuron neuron = new Neuron(network, rule);
    // ((BiasedUpdateRule) neuron.getUpdateRule()).setBias(0);
    // rule.setLowerBound(0);
    // rule.setUpperBound(1);
    // network.addLooseNeuron(neuron);
    // // System.out.println("Output " + i + " = " + neuron.getId());
    // outputLayer.add(neuron);
    // }
    //
    // // Connect input layer to output layer
    // AllToAll connection = new AllToAll(network, inputLayer, outputLayer);
    // connection.connectNeurons(true);
    //
    // // Set initial weights (from an Emergent sim)
    // Network.getSynapse(inputLayer.get(0), outputLayer.get(0)).setStrength(
    // .352391);
    // Network.getSynapse(inputLayer.get(1), outputLayer.get(0)).setStrength(
    // .354468);
    // Network.getSynapse(inputLayer.get(2), outputLayer.get(0)).setStrength(
    // .338344);
    // Network.getSynapse(inputLayer.get(3), outputLayer.get(0)).setStrength(
    // .3593);
    // Network.getSynapse(inputLayer.get(0), outputLayer.get(1)).setStrength(
    // .561543);
    // Network.getSynapse(inputLayer.get(1), outputLayer.get(1)).setStrength(
    // .584706);
    // Network.getSynapse(inputLayer.get(2), outputLayer.get(1)).setStrength(
    // .355258);
    // Network.getSynapse(inputLayer.get(3), outputLayer.get(1)).setStrength(
    // .555266);
    //
    // // Initialize the trainer
    // // REDO
    // // LMSIterative trainer = new LMSIterative(network, inputLayer,
    // // outputLayer);
    // // network.setInputData(inputData);
    // // trainer.setTrainingData(trainingData);
    // // int epochs = 1000; // Error gets low with 1000 epochs
    // // for (int i = 0; i < epochs; i++) {
    // // trainer.apply();
    // // //System.out.println(network);
    // // System.out.println("Epoch " + i + ", error = "
    // // + ((IterableAlgorithm) trainer).getError());
    // // }
    // }
    //
    // /**
    // * A simple AND gate.
    // */
    // public static void test() {
    //
    // double inputData[][] = { { 1, 1 }, { -1, 1 }, { 1, -1 }, { -1, -1 } };
    // double trainingData[][] = { { 1 }, { 0 }, { 0 }, { 0 } };
    //
    // // TODO: Long API! Must be shortcuts...
    //
    // // Build network
    // Network network = new Network();
    //
    // // Set up input layer
    // List<Neuron> inputLayer = new ArrayList<Neuron>();
    // for (int i = 0; i < 2; i++) {
    // Neuron neuron = new Neuron(network, new LinearRule());
    // network.addLooseNeuron(neuron);
    // inputLayer.add(neuron);
    // // System.out.println("Input " + i + " = " + neuron.getId());
    // }
    //
    // // Set up output layer
    // List<Neuron> outputLayer = new ArrayList<Neuron>();
    // for (int i = 0; i < 1; i++) {
    // LinearRule rule = new LinearRule();
    // Neuron neuron = new Neuron(network, rule);
    // ((BiasedUpdateRule) neuron.getUpdateRule()).setBias(0);
    // rule.setLowerBound(0);
    // rule.setUpperBound(1);
    // network.addLooseNeuron(neuron);
    // // System.out.println("Output " + i + " = " + neuron.getId());
    // outputLayer.add(neuron);
    // }
    //
    // // Connect input layer to output layer
    // AllToAll connection = new AllToAll(network, inputLayer, outputLayer);
    // connection.connectNeurons(true);
    //
    // // Set initial weights
    // network.randomizeWeights();
    //
    // // Initialize the trainer
    // // REDO
    // // LMSIterative trainer = new LMSIterative(network, inputLayer,
    // // outputLayer);
    // // trainer.learningRate = .01;
    // // trainer.setInputData(inputData);
    // // trainer.setTrainingData(trainingData);
    // // int epochs = 1000;
    // // for (int i = 0; i < epochs; i++) {
    // // trainer.apply();
    // // //System.out.println(network);
    // // System.out.println("Epoch " + i + ", error = "
    // // + ((IterableAlgorithm) trainer).getError());
    // // }
    // }

}
