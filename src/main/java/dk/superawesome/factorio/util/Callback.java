package dk.superawesome.factorio.util;

import java.util.ArrayList;
import java.util.List;

public class Callback {

    private final List<Runnable> call = new ArrayList<>();

    public void add(Runnable action) {
        this.call.add(action);
    }

    public void call() {
        for (Runnable action : call) {
            action.run();
        }
    }
}
