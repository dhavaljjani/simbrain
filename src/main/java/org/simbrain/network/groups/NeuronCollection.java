/*
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
package org.simbrain.network.groups;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.simbrain.network.LocatableModel;
import org.simbrain.network.core.*;
import org.simbrain.network.dl4j.ArrayConnectable;
import org.simbrain.network.dl4j.WeightMatrix;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.UserParameter;
import org.simbrain.util.Utils;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;

import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A collection of loose neurons (neurons in a {@link NeuronGroup} can be added to a collection).
 * Allows them to be labelled, moved around as a unit, coupled to, etc.   However no special processing
 * occurs in neuron collections.  They are a convenience.  NeuronCollections can overlap each other.
 */
public class NeuronCollection extends AbstractNeuronCollection {

    private PropertyChangeListener networkListener;

    /**
     * Construct a new neuron group from a list of neurons.
     *
     * @param net     the network
     * @param neurons the neurons
     */
    public NeuronCollection(final Network net, final List<Neuron> neurons) {
        super(net);
        addNeurons(neurons);
        initializeId();
        PropertyChangeListener networkListener = evt -> {
            if ("neuronRemoved".equals(evt.getPropertyName())) {
                Neuron removed = (Neuron) evt.getOldValue();
                removeNeuron(removed);
                firePositionChanged();
                // If that was the last neuron in the list, remove the neuron collection
                if (isEmpty()) {
                    delete();
                }
            }
        };
        neurons.forEach(n -> {
            n.addPropertyChangeListener(evt -> {
                if ("moved".equals(evt.getPropertyName())) {
                    firePositionChanged();
                }
            });
        });
        net.addPropertyChangeListener(networkListener);
    }

    /**
     * Translate all neurons (the only objects with position information).
     *
     * @param offsetX x offset for translation.
     * @param offsetY y offset for translation.
     */
    public void offset(final double offsetX, final double offsetY) {
        for (Neuron neuron : getNeuronList()) {
            neuron.setX(neuron.getX() + offsetX);
            neuron.setY(neuron.getY() + offsetY);
        }
        firePositionChanged();
    }

    public void addNeuron(Neuron neuron, boolean fireEvent) {
        addNeuron(neuron);
        if (fireEvent) {
            changeSupport.firePropertyChange("add", this, null);
        }
    }

    @Override
    public void update() {
        // TODO
    }

    /**
     * Call after deleting neuron collection from parent network.
     */
    public void delete() {
        changeSupport.firePropertyChange("delete", this, null);
        getParentNetwork().removePropertyChangeListener(networkListener);
        fireDeleted();
    }

    @Override
    public String getUpdateMethodDescription() {
        // TODO
        return null;
    }

    /**
     * Set the update rule for the neurons in this group.
     *
     * @param base the neuron update rule to set.
     */
    public void setNeuronType(NeuronUpdateRule base) {
        getNeuronList().forEach(n -> n.setUpdateRule(base.deepCopy()));
    }

    /**
     * Set the string update rule for the neurons in this group.
     *
     * @param rule the neuron update rule to set.
     */
    public void setNeuronType(String rule) {
        try {
            NeuronUpdateRule newRule = (NeuronUpdateRule) Class.forName("org.simbrain.network.neuron_update_rules." + rule).newInstance();
            setNeuronType(newRule);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setLocation(Point2D location) {
        // TODO
    }


    @Override
    public String toString() {
        String ret = new String();
        ret += ("Neuron Collection [" + getLabel() + "]. Neuron collection with " + this.getNeuronList().size() + " neuron(s)" + ". Located at (" + Utils.round(this.getPosition().x, 2) + "," + Utils.round(this.getPosition().y, 2) + ").\n");
        return ret;
    }

    /**
     * Returns the summed hash codes of contained neurons.  Used to prevent creating neuron collections
     * from identical neurons.
     *
     * @return summed hash
     */
    public int getSummedNeuronHash() {
        return getNeuronList().stream().mapToInt(Object::hashCode).sum();
    }

    @Override
    public EditableObject copy() {
        // TODO
        return null;
    }

}