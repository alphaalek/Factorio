package dk.superawesome.factories.mehcanics;

public interface Mechanic<M extends Mechanic<M>> {

    MechanicProfile<M> getProfile();
}
