package dk.superawesome.factories.production;

import dk.superawesome.factories.production.impl.Constructor;
import dk.superawesome.factories.util.Array;

public class ProductionTypes {

    public static ProductionType CONSTRUCTOR;

    static {
        CONSTRUCTOR = loadProduction(new Constructor());
    }

    private static final Array<ProductionType> productions = new Array<>();

    public static ProductionType loadProduction(ProductionType production) {
        productions.set(production, production);
        return production;
    }

    public static Array<ProductionType> getProductions() {
        return productions;
    }
}
