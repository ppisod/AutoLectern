package sys.exe.al.util.tool;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ToolsUtility {

    public static boolean toolNearBreak (final ItemStack tool) {
        return tool.isDamageable() && tool.getDamage() + 2 >= tool.getMaxDamage();
    }

    public static boolean EquipAxes (final @NotNull ClientPlayerEntity plr) {
        final var inventory = plr.getInventory();
        for(int i = 0; i < 9; ++i) {
            final var stack = inventory.getStack(i);
            if(!(stack.getItem() instanceof AxeItem))
                continue;
            if(toolNearBreak(stack))
                continue;
            inventory.setSelectedSlot(i);
            return true;
        }
        return false;
    }

    public static boolean CheckForToolAvailability (final @NotNull ClientPlayerEntity p) {
        final var tool = p.getMainHandStack();
        if(!ToolsUtility.toolNearBreak(tool))
            return true;
        return ToolsUtility.EquipAxes(p);
    }

}
