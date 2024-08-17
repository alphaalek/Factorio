package dk.superawesome.factorio.mechanics.stackregistry;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.Arrays;
import java.util.Optional;

public enum Filled {

    WATER_BUCKET(Volume.BUCKET, Fluid.WATER),
    LAVA_BUCKET(Volume.BUCKET, Fluid.LAVA),
    WATER_BOTTLE(Volume.BOTTLE, Fluid.WATER);

    private final Volume volume;
    private final Fluid fluid;

    Filled(Volume volume, Fluid fluid) {
        this.volume = volume;
        this.fluid = fluid;
    }

    public static Optional<Filled> getFilledState(Volume volume, Fluid fluid) {
        return Arrays.stream(values())
            .filter(filled -> filled.getVolume().equals(volume) && filled.getFluid().equals(fluid))
            .findFirst();
    }

    public static Optional<Filled> getFilledStateByOutputItemStack(ItemStack itemStack) {
        return Arrays.stream(values())
            .filter(filled -> filled.getOutputItemStack().isSimilar(itemStack))
            .findFirst();
    }

    public ItemStack getOutputItemStack() {
        return switch (this) {
            case WATER_BUCKET -> new ItemStack(Material.WATER_BUCKET);
            case LAVA_BUCKET -> new ItemStack(Material.LAVA_BUCKET);
            case WATER_BOTTLE -> {
                ItemStack bottle = new ItemStack(Material.POTION);
                ItemMeta meta = bottle.getItemMeta();
                ((PotionMeta) meta).setBasePotionData(new PotionData(PotionType.WATER));
                bottle.setItemMeta(meta);
                yield bottle;
            }
        };
    }

    public Volume getVolume() {
        return volume;
    }

    public Fluid getFluid() {
        return fluid;
    }
}
