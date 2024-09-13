package dk.superawesome.factorio.mechanics.routes;

import dk.superawesome.factorio.mechanics.routes.events.pipe.PipeBuildEvent;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipeRemoveEvent;
import dk.superawesome.factorio.mechanics.routes.events.signal.SignalBuildEvent;
import dk.superawesome.factorio.mechanics.routes.events.signal.SignalRemoveEvent;
import dk.superawesome.factorio.mechanics.routes.impl.Pipe;
import dk.superawesome.factorio.mechanics.routes.impl.Signal;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BlockVector;

public interface RouteFactory<R extends AbstractRoute<R, ?>> {

    interface EventHandler<R extends AbstractRoute<R, ?>> {

        void callBuildEvent(R route);

        void callRemoveEvent(R route);
    }

    R create(BlockVector start, World world);

    EventHandler<R> getEventHandler();

    default BlockFace[] getRelatives() {
        return Routes.RELATIVES;
    }

    class PipeRouteFactory implements RouteFactory<Pipe> {

        public static final PipeRouteFactory FACTORY = new PipeRouteFactory();

        @Override
        public Pipe create(BlockVector start, World world) {
            return new Pipe(start, world);
        }

        @Override
        public EventHandler<Pipe> getEventHandler() {
            return new EventHandler<>() {
                @Override
                public void callBuildEvent(Pipe route) {
                    Bukkit.getPluginManager().callEvent(new PipeBuildEvent(route));
                }

                @Override
                public void callRemoveEvent(Pipe route) {
                    Bukkit.getPluginManager().callEvent(new PipeRemoveEvent(route));
                }
            };
        }
    }

    class SignalRouteFactory implements RouteFactory<Signal> {

        public static final SignalRouteFactory FACTORY = new SignalRouteFactory();

        @Override
        public Signal create(BlockVector start, World world) {
            return new Signal(start, world);
        }

        @Override
        public EventHandler<Signal> getEventHandler() {
            return new EventHandler<>() {
                @Override
                public void callBuildEvent(Signal route) {
                    Bukkit.getPluginManager().callEvent(new SignalBuildEvent(route));
                }

                @Override
                public void callRemoveEvent(Signal route) {
                    Bukkit.getPluginManager().callEvent(new SignalRemoveEvent(route));
                }
            };
        }

        @Override
        public BlockFace[] getRelatives() {
            return Routes.SIGNAL_EXPAND_DIRECTIONS;
        }
    }
}
