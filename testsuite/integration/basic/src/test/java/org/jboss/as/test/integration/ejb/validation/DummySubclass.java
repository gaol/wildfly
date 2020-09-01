package org.jboss.as.test.integration.ejb.validation;

import javax.validation.constraints.NotNull;

public class DummySubclass extends DummyAbstractClass {

    @NotNull
    private String direction;

    public int getSpeed() {
        return speed;
    }

    public String getDirection() {
        return direction;
    }

    public void setSpeed(int number) {
        this.speed = number;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}
