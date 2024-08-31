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

public class Splitter extends Circuit<Splitter, TransferCollection> implements Container<TransferCollection> {

    private final List<Block> outputBlocks = new ArrayList<>();
    private int currentStartIndex;

    private boolean validating;

    public Splitter(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    public MechanicProfile<Splitter> getProfile() {
        return Profiles.SPLITTER;
    }

    @Override
    public void onBlocksLoaded(Player by) {
        setupNearby();
    }

    private void setupNearby() {
        BlockUtil.forRelative(loc.getBlock(), block -> {
            if (block.getType() == Material.PISTON && BlockUtil.getPointingBlock(block, false).equals(loc.getBlock())) {
                // ignore input block
                return;
            }

            setup(block);
        });
    }

    private void setup(Block block) {
        Routes.setupForcibly(block, Routes.transferRouteFactory, true);
        validating = true;
        Routes.removeNearbyRoutes(block);
        validating = false;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (BlockUtil.isRelativeFast(event.getBlock(), loc.getBlock())) {
            if (event.getBlock().getType() == Material.PISTON && BlockUtil.getPointingBlock(event.getBlock(), false).equals(loc.getBlock())) {
                // ignore input block
                return;
            }

            setup(event.getBlock());
        }
    }

    @EventHandler
    public void onPipeBuild(PipeBuildEvent event) {
        if (!outputBlocks.contains(BlockUtil.getBlock(event.getRoute().getWorld(), event.getRoute().getStart()))
                && !event.getRoute().getOutputs(Routes.DEFAULT_CONTEXT).isEmpty()) {
            // iterate over blocks nearby and check if the route origin block is matching the block at the location of this splitter
            BlockUtil.forRelative(BlockUtil.getBlock(loc.getWorld(), event.getRoute().getStart()), b -> {
                if (loc.getBlock().equals(b)) {
                    outputBlocks.add(BlockUtil.getBlock(loc.getWorld(), event.getRoute().getStart()));
                }
            });
        }
    }

    @EventHandler
    public void onPipeRemove(PipeRemoveEvent event) {
        if (!validating) {
            outputBlocks.clear();
            setupNearby();
        }
    }

    @Override
    public boolean accepts(TransferCollection collection) {
        return collection instanceof ItemCollection || collection instanceof MoneyCollection;
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
    public boolean pipePut(TransferCollection collection) {
        if (outputBlocks.isEmpty()) {
            return false;
        }

        int total = Math.min(collection.getMaxTransfer() * outputBlocks.size(), collection.getTransferAmount());
        int each = (int) Math.floor(((double) total) / outputBlocks.size());
        AtomicInteger remainder = new AtomicInteger(total - each * outputBlocks.size());

        Iterator<Block> blockIterator = remainder.get() > 0 ? createEvenRemainderDistribution() : new ArrayList<>(outputBlocks).iterator();

        boolean transferred = false;
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

                if (Routes.startTransferRoute(block, wrappedCollection, this, true)) {
                    transferred = true;
                }
            }
        }

        return transferred;
    }

    @Override
    public int getCapacity() {
        return -1;
    }
}

