package dk.superawesome.factorio.mechanics.routes;

import dk.superawesome.factorio.mechanics.routes.events.pipe.PipeBuildEvent;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipeRemoveEvent;
import dk.superawesome.factorio.mechanics.routes.events.signal.SignalBuildEvent;
import dk.superawesome.factorio.mechanics.routes.events.signal.SignalRemoveEvent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.util.BlockVector;

public interface RouteFactory<R extends AbstractRoute<R, ? extends OutputEntry>> {

    R create(BlockVector start, World world);

    void callBuildEvent(R route);

    void callRemoveEvent(R route);

    class PipeRouteFactory implements RouteFactory<AbstractRoute.Pipe> {

        @Override
        public AbstractRoute.Pipe create(BlockVector start, World world) {
            return new AbstractRoute.Pipe(start, world);
        }

        @Override
        public void callBuildEvent(AbstractRoute.Pipe route) {
            Bukkit.getPluginManager().callEvent(new PipeBuildEvent(route));
        }

        @Override
        public void callRemoveEvent(AbstractRoute.Pipe route) {
            Bukkit.getPluginManager().callEvent(new PipeRemoveEvent(route));
        }
    }

    class SignalRouteFactory implements RouteFactory<AbstractRoute.Signal> {

        @Override
        public AbstractRoute.Signal create(BlockVector start, World world) {
            return new AbstractRoute.Signal(start, world);
        }

        @Override
        public void callBuildEvent(AbstractRoute.Signal route) {
            Bukkit.getPluginManager().callEvent(new SignalBuildEvent(route));
        }

        @Override
        public void callRemoveEvent(AbstractRoute.Signal route) {
            Bukkit.getPluginManager().callEvent(new SignalRemoveEvent(route));
        }
    }
}
