package org.jboss.as.test.integration.ejb.validation;

import javax.validation.constraints.Min;

public abstract class DummyAbstractClass {

    @Min(1)
    protected int speed;

    public abstract int getSpeed();

    public abstract void setSpeed(int speed);
}
