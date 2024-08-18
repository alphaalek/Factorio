package dk.superawesome.factorio.mechanics.stackregistry;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

public enum Filled {

    WATER_BUCKET(Volume.BUCKET, Fluid.WATER, () -> new ItemStack(Material.WATER_BUCKET)),
    LAVA_BUCKET(Volume.BUCKET, Fluid.LAVA, () -> new ItemStack(Material.LAVA_BUCKET)),
    SNOW_BUCKET(Volume.BUCKET, Fluid.SNOW, () -> new ItemStack(Material.POWDER_SNOW_BUCKET)),
    WATER_BOTTLE(Volume.BOTTLE, Fluid.WATER, () -> {
        ItemStack bottle = new ItemStack(Material.POTION);
        ItemMeta meta = bottle.getItemMeta();
        ((PotionMeta) meta).setBasePotionType(PotionType.WATER);
        bottle.setItemMeta(meta);
        return bottle;
    });

    private final Volume volume;
    private final Fluid fluid;
    private final Supplier<ItemStack> stack;

    Filled(Volume volume, Fluid fluid, Supplier<ItemStack> stack) {
        this.volume = volume;
        this.fluid = fluid;
        this.stack = stack;
    }

    public static Optional<Filled> getFilledState(Volume volume, Fluid fluid) {
        return Arrays.stream(values())
                .filter(filled -> filled.getVolume().equals(volume) && filled.getFluid().equals(fluid))
                .findFirst();
    }

    public static Optional<Filled> getFilledStateByStack(ItemStack itemStack) {
        return Arrays.stream(values())
                .filter(filled -> filled.getOutputItemStack().isSimilar(itemStack))
                .findFirst();
    }

    public ItemStack getOutputItemStack() {
        return stack.get();
    }

    public Volume getVolume() {
        return volume;
    }

    public Fluid getFluid() {
        return fluid;
    }
}
