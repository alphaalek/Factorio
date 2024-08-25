package dk.superawesome.factorio.mechanics.impl.circuits;

import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.AbstractRoute;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipeBuildEvent;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipeRemoveEvent;
import dk.superawesome.factorio.mechanics.transfer.Container;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.mechanics.transfer.MoneyCollection;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class Splitter extends AbstractMechanic<Splitter> implements Container<TransferCollection> {

    private final List<Block> outputBlocks = new ArrayList<>();
    private int currentStartIndex;

    private boolean validating;

    public Splitter(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public MechanicProfile<Splitter> getProfile() {
        return Profiles.SPLITTER;
    }

    @Override
    public void onBlocksLoaded(Player by) {
        for (BlockFace face : Routes.RELATIVES) {
            Block block = BlockUtil.getRel(loc, face.getDirection()).getBlock();
            if (block.getType() == Material.PISTON && BlockUtil.getPointingBlock(block, true).equals(loc.getBlock())) {
                // ignore input block
                continue;
            }

            setup(block);
        }
    }

    private void setup(Block block) {
        Routes.setupForcibly(block, Routes.transferRouteFactory, true);
        validating = true;
        Routes.removeNearbyRoutes(block);
        validating = false;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        for (BlockFace face : Routes.RELATIVES) {
            Block block = loc.getBlock().getRelative(face);
            if (block.equals(event.getBlock())) {
                setup(event.getBlock());
                break;
            }
        }
    }

    @EventHandler
    public void onPipeBuild(PipeBuildEvent event) {
        Bukkit.broadcastMessage("pipe build " + event.getRoute().getStart());
        if (!event.getRoute().getOutputs(Routes.DEFAULT_CONTEXT).isEmpty()) {
            // iterate over blocks nearby and check if the route origin block is matching the block at the location of this splitter
            for (BlockFace face : Routes.RELATIVES) {
                if (event.getRoute().getStart().equals(BlockUtil.getVec(BlockUtil.getRel(loc, face.getDirection())))
                        // check if the output blocks already registered does not connect to same route as the one built
                        && outputBlocks.stream()
                                .map(BlockUtil::getVec)
                                .flatMap(v -> AbstractRoute.getCachedRoutes(loc.getWorld(), v).stream())
                                .flatMap(r -> r.getLocations().stream())
                                .noneMatch(event.getRoute().getStart()::equals)) {
                    outputBlocks.add(BlockUtil.getBlock(loc.getWorld(), event.getRoute().getStart()));
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onPipeRemove(PipeRemoveEvent event) {
        if (!validating) {
            outputBlocks.removeIf(b -> event.getRoute().getStart().equals(BlockUtil.getVec(b)));
        }
    }

    @Override
    public boolean accepts(TransferCollection collection) {
        return false;
    }

    @Override
    public boolean isContainerEmpty() {
        return true;
    }

    private Iterator<Block> createEvenRemainderDistribution() {
        return new Iterator<>() {

            final int endIndexExclusive = currentStartIndex + outputBlocks.size();
            int currentIndex = currentStartIndex;

            {
                currentStartIndex++;
            }

            @Override
            public boolean hasNext() {
                return currentIndex < endIndexExclusive;
            }

            @Override
            public Block next() {
                return outputBlocks.get(currentIndex++ % outputBlocks.size());
            }
        };
    }

    @Override
    public void pipePut(TransferCollection collection, Set<AbstractRoute.Pipe> route, PipePutEvent event) {
        Bukkit.broadcastMessage("Put " + outputBlocks);

        if (outputBlocks.isEmpty()) {
            return;
        }

        int total = Math.min(collection.getMaxTransfer() * outputBlocks.size(), collection.getTransferAmount());
        int each = (int) Math.floor(((double) total) / outputBlocks.size());
        AtomicInteger remainder = new AtomicInteger(total - each * outputBlocks.size());

        Iterator<Block> blockIterator = remainder.get() > 0 ? createEvenRemainderDistribution() : outputBlocks.iterator();
        while (blockIterator.hasNext()) {
            Block block = blockIterator.next();

            if (!collection.isTransferEmpty()) {
                TransferCollection wrappedCollection;
                if (collection instanceof ItemCollection itemCollection) {
                    wrappedCollection = new ItemCollection() {
                        @Override
                        public boolean has(ItemStack stack) {
                            return itemCollection.has(stack);
                        }

                        @Override
                        public boolean has(Predicate<ItemStack> stack) {
                            return itemCollection.has(stack);
                        }

                        @Override
                        public List<ItemStack> take(int amount) {
                            return itemCollection.take(Math.min(amount, each + Math.max(0, remainder.getAndDecrement())));
                        }

                        @Override
                        public boolean isTransferEmpty() {
                            return itemCollection.isTransferEmpty();
                        }

                        @Override
                        public DelayHandler getTransferDelayHandler() {
                            return itemCollection.getTransferDelayHandler();
                        }

                        @Override
                        public int getMaxTransfer() {
                            return itemCollection.getMaxTransfer();
                        }

                        @Override
                        public int getTransferAmount() {
                            return itemCollection.getTransferAmount();
                        }

                        @Override
                        public double getTransferEnergyCost() {
                            return itemCollection.getTransferEnergyCost();
                        }
                    };
                } else if (collection instanceof MoneyCollection moneyCollection) {
                    wrappedCollection = new MoneyCollection() {
                        @Override
                        public double take(double amount) {
                            return moneyCollection.take(amount);
                        }

                        @Override
                        public boolean isTransferEmpty() {
                            return moneyCollection.isTransferEmpty();
                        }

                        @Override
                        public DelayHandler getTransferDelayHandler() {
                            return moneyCollection.getTransferDelayHandler();
                        }

                        @Override
                        public int getMaxTransfer() {
                            return moneyCollection.getMaxTransfer();
                        }

                        @Override
                        public int getTransferAmount() {
                            return moneyCollection.getTransferAmount();
                        }

                        @Override
                        public double getTransferEnergyCost() {
                            return moneyCollection.getTransferEnergyCost();
                        }
                    };
                } else continue;

                boolean transferred = Routes.startTransferRoute(block, route, wrappedCollection, this, true);
                if (!event.transferred()) {
                    event.setTransferred(transferred);
                }
            }
        }
    }

    @Override
    public int getCapacity() {
        return -1;
    }
}

