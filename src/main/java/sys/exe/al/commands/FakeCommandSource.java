package sys.exe.al.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class FakeCommandSource extends ClientCommandSource {

    public final MinecraftClient mc;
    private final ClientPlayerEntity player;
    public FakeCommandSource(final MinecraftClient mc, final ClientPlayerEntity player) {
        super(mc.getNetworkHandler(), mc, (perm) -> true);
        this.mc = mc;
        this.player = player;
    }

    @Override
    public CompletableFuture<Suggestions> getCompletions(CommandContext<?> context) {
        return Suggestions.empty();
    }

    public void sendMessage(Text message) {
        this.player.sendMessage(message, false);
    }
    @Override
    public Collection<String> getPlayerNames() {
        final var networkHandler = mc.getNetworkHandler();
        if(networkHandler == null)
            return new ArrayList<>();
        return networkHandler.getPlayerList().stream().map(e -> e.getProfile().name()).collect(Collectors.toList());
    }
}