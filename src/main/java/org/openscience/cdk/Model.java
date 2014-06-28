package org.openscience.cdk;

import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.interfaces.IAtomContainer;

import javax.vecmath.Point2d;

/**
 * Model – currently only holds the chemical structure representation (CDK 
 * AtomContainer).
 * 
 * @author John May
 */
final class Model {
    
    final IAtomContainer container;

    private Model(IAtomContainer container) {
        this.container = container;
    }
    
    static Model create(IAtomContainer container) {
        return new Model(container);    
    }

    IAtomContainer rotatedByDegree(final double theta) {
        return rotatedByRadians(Math.toRadians(theta));
    }
    
    IAtomContainer rotatedByRadians(final double theta) {
        try {
            final IAtomContainer cpy    = container.clone();
            final Point2d        center = GeometryTools.get2DCenter(cpy);
            GeometryTools.rotate(cpy, center, theta);
            return cpy;
        } catch (CloneNotSupportedException e) {
            // no clone not supported is thrown internally
            throw new IllegalStateException("Clone not supported");
        }
    }
}
