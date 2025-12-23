package sys.exe.al.util.villager;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class VillagerUtility {

    @SuppressWarnings("ManualMinMaxCalculation")
    public static int getMostExpensiveVillagerEnchant (final RegistryEntry<Enchantment> enchant) {
        final var k = enchant.value().getMaxLevel();
        int minPrice = (k * 3) + 2 + (4 + k * 10);
        if (enchant.isIn(EnchantmentTags.DOUBLE_TRADE_PRICE))
            minPrice *= 2;
        return (minPrice > 64) ? 64 : minPrice;
    }
    @SuppressWarnings("ManualMinMaxCalculation")
    public static int getCheapestVillagerEnchant (final RegistryEntry<Enchantment> enchant) {
        int minPrice = (enchant.value().getMaxLevel() * 3) + 2;
        if (enchant.isIn(EnchantmentTags.DOUBLE_TRADE_PRICE))
            minPrice *= 2;
        return (minPrice > 64) ? 64 : minPrice;
    }

}
