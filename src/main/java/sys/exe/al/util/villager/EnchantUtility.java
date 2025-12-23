package sys.exe.al.util.villager;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import sys.exe.al.ALGoal;

import java.util.ArrayList;

public class EnchantUtility {

    public static @Nullable RegistryEntry<Enchantment> enchantFromIdentifier (final World world, final Identifier id) {
        final var enchants = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        final var enchant = enchants.get(id);
        if(enchant == null)
            return null;
        return enchants.getEntry(enchant);
    }

    public static boolean isGoalLevelMet(final int maxEncLvl, final int lvlMin, final int lvlMax, final int lvl) {
        if(lvlMin == -1) {
            if(lvlMax == -1)
                return true;
            return lvl <= 1;
        }
        if(lvlMax == -1)
            return lvl >= maxEncLvl;
        return lvl >= lvlMin && lvl <= lvlMax;
    }

    public static boolean isGoalPriceMet(final RegistryEntry<Enchantment> enchant, final int priceMin, final int priceMax, final int price) {
        if(priceMin == -1) {
            if(priceMax == -1)
                return true;
            return price <= VillagerUtility.getCheapestVillagerEnchant(enchant);
        }
        if(priceMax == -1)
            return price >= VillagerUtility.getMostExpensiveVillagerEnchant(enchant);
        return price >= priceMin && price <= priceMax;
    }

    public static int getGoalMet (final World world, final int price, final Identifier enchant, final int lvl, ArrayList<ALGoal> goals) {
        int idx = 0;
        for(final var curGoal : goals) {
            final var enc_id = curGoal.enchant();
            if(enc_id == null)
                continue;
            final var enc = enchantFromIdentifier(world, enc_id);
            if(enc == null)
                continue;
            if (enchant.equals(enc_id) &&
                    isGoalLevelMet(
                            enc.value().getMaxLevel(),
                            curGoal.lvlMin(),
                            curGoal.lvlMax(),
                            lvl
                    ) && isGoalPriceMet(
                    enc,
                    curGoal.priceMin(),
                    curGoal.priceMax(),
                    price
            ))
                return idx;
            ++idx;
        }
        return -1;
    }
}
