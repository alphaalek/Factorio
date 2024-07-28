package dk.superawesome.factorio.mechanics.routes;

public interface RouteFactory<R extends AbstractRoute<R, ? extends OutputEntry>> {

    R create();

    class PipeRouteFactory implements RouteFactory<AbstractRoute.Pipe> {

        @Override
        public AbstractRoute.Pipe create() {
            return new AbstractRoute.Pipe();
        }
    }

    class SignalRouteFactory implements RouteFactory<AbstractRoute.Signal> {

        @Override
        public AbstractRoute.Signal create() {
            return new AbstractRoute.Signal();
        }
    }
}
