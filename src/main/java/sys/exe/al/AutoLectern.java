package sys.exe.al;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sys.exe.al.commands.AutoLec;
import sys.exe.al.commands.ClientCommandManager;
import sys.exe.al.helper.AutoLecterner;

import java.io.File;

public class AutoLectern implements ClientModInitializer {
    private static AutoLectern INSTANCE;
    public static AutoLectern getInstance() {
        return INSTANCE;
    }

    public AutoLecterner lec;

    public static final Logger LOGGER = LoggerFactory.getLogger("Auto Lectern");

    public void MinecraftTickHead (final MinecraftClient mc) {
        lec.Tick(mc);
    }

    public static void registerCommands(CommandDispatcher<ClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        ClientCommandManager.clearClientSideCommands();
        AutoLec.register(dispatcher, registryAccess);
    }


    public void saveConfig(){
        lec.Config.save();
    }

    @Override
    public void onInitializeClient() {
        File configFile = FabricLoader.getInstance().getConfigDir().resolve("autolec.txt").toFile();
        lec = new AutoLecterner(configFile);
    }
}
