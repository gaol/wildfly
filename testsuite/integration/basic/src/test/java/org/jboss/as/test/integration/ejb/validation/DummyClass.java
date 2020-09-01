package org.jboss.as.test.integration.ejb.validation;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class DummyClass {

    @NotNull
    private String direction;

    @Min(1)
    protected int speed;

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int number) {
        this.speed = number;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}
