package sys.exe.al.util.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class PlayerUtility {

    @Nullable
    public static BlockHitResult GetBlockInView (final ClientPlayerEntity plr, float pitch, float yaw) {
        final var oldPitch = plr.getPitch();
        final var oldYaw = plr.getYaw();
        plr.setPitch(pitch);
        plr.setYaw(yaw);
        final var hitResult = plr.raycast(4.5f, 0, false);
        plr.setPitch(oldPitch);
        plr.setYaw(oldYaw);
        if(hitResult.getType() != HitResult.Type.BLOCK)
            return null;
        return (BlockHitResult) hitResult;
    }

    public static BlockHitResult GetBlockLookingAt (MinecraftClient mc) {
        final var target = mc.crosshairTarget;
        if (!(target instanceof final BlockHitResult hitResult)) {
            return null;
        }
        return hitResult;
    }

    public static boolean PlayerValid (ClientPlayerEntity player) {
        return player != null && player.currentScreenHandler instanceof PlayerScreenHandler;
    }

}
