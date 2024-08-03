package dk.superawesome.factorio.mechanics.routes;

import org.bukkit.util.BlockVector;

public interface RouteFactory<R extends AbstractRoute<R, ? extends OutputEntry>> {

    R create(BlockVector start);

    class PipeRouteFactory implements RouteFactory<AbstractRoute.Pipe> {

        @Override
        public AbstractRoute.Pipe create(BlockVector start) {
            return new AbstractRoute.Pipe(start);
        }
    }

    class SignalRouteFactory implements RouteFactory<AbstractRoute.Signal> {

        @Override
        public AbstractRoute.Signal create(BlockVector start) {
            return new AbstractRoute.Signal(start);
        }
    }
}
