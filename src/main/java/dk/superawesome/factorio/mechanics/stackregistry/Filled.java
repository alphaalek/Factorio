package dk.superawesome.factorio.mechanics.stackregistry;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

public enum Filled {

    WATER_BUCKET(Volume.BUCKET, Fluid.WATER, Sound.ITEM_BUCKET_FILL, () -> new ItemStack(Material.WATER_BUCKET)),
    LAVA_BUCKET(Volume.BUCKET, Fluid.LAVA, Sound.ITEM_BUCKET_FILL_LAVA, () -> new ItemStack(Material.LAVA_BUCKET)),
    SNOW_BUCKET(Volume.BUCKET, Fluid.SNOW, Sound.ITEM_BUCKET_FILL_POWDER_SNOW, () -> new ItemStack(Material.POWDER_SNOW_BUCKET)),
    WATER_BOTTLE(Volume.BOTTLE, Fluid.WATER, Sound.ITEM_BOTTLE_FILL, () -> {
        ItemStack bottle = new ItemStack(Material.POTION);
        ItemMeta meta = bottle.getItemMeta();
        ((PotionMeta) meta).setBasePotionType(PotionType.WATER);
        bottle.setItemMeta(meta);
        return bottle;
    });

    private final Volume volume;
    private final Fluid fluid;
    private final Sound fillSound;
    private final Supplier<ItemStack> stack;

    Filled(Volume volume, Fluid fluid, Sound fillSound, Supplier<ItemStack> stack) {
        this.volume = volume;
        this.fluid = fluid;
        this.fillSound = fillSound;
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

    public Sound getFillSound() {
        return fillSound;
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
