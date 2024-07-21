package dk.superawesome.factories.mechanics.routes;

public interface RouteFactory<R extends AbstractRoute<R, P>, P extends OutputEntry> {

    R create();

    class PipeRouteFactory implements RouteFactory<AbstractRoute.Pipe, AbstractRoute.ItemsOutputEntry> {

        @Override
        public AbstractRoute.Pipe create() {
            return new AbstractRoute.Pipe();
        }
    }

    class SignalRouteFactory implements RouteFactory<AbstractRoute.Signal, AbstractRoute.SignalOutputEntry> {

        @Override
        public AbstractRoute.Signal create() {
            return new AbstractRoute.Signal();
        }
    }
}
