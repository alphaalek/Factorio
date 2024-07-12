package dk.superawesome.factories.production.impl;

import dk.superawesome.factories.production.ProductionType;

public class Constructor implements ProductionType {

    @Override
    public int getID() {
        return 0;
    }

    @Override
    public String getName() {
        return "Constructor";
    }
}
