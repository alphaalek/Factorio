package dk.superawesome.factorio.mechanics.routes;

import dk.superawesome.factorio.mechanics.routes.events.pipe.PipeBuildEvent;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipeRemoveEvent;
import dk.superawesome.factorio.mechanics.routes.events.signal.SignalBuildEvent;
import dk.superawesome.factorio.mechanics.routes.events.signal.SignalRemoveEvent;
import dk.superawesome.factorio.mechanics.routes.impl.Pipe;
import dk.superawesome.factorio.mechanics.routes.impl.Signal;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.util.BlockVector;

public interface RouteFactory<R extends AbstractRoute<R, ?>> {

    R create(BlockVector start, World world);

    void callBuildEvent(R route);

    void callRemoveEvent(R route);

    class PipeRouteFactory implements RouteFactory<Pipe> {

        @Override
        public Pipe create(BlockVector start, World world) {
            return new Pipe(start, world);
        }

        @Override
        public void callBuildEvent(Pipe route) {
            Bukkit.getPluginManager().callEvent(new PipeBuildEvent(route));
        }

        @Override
        public void callRemoveEvent(Pipe route) {
            Bukkit.getPluginManager().callEvent(new PipeRemoveEvent(route));
        }
    }

    class SignalRouteFactory implements RouteFactory<Signal> {

        @Override
        public Signal create(BlockVector start, World world) {
            return new Signal(start, world);
        }

        @Override
        public void callBuildEvent(Signal route) {
            Bukkit.getPluginManager().callEvent(new SignalBuildEvent(route));
        }

        @Override
        public void callRemoveEvent(Signal route) {
            Bukkit.getPluginManager().callEvent(new SignalRemoveEvent(route));
        }
    }
}
