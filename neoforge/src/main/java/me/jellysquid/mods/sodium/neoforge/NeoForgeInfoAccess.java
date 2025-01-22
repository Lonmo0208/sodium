package me.jellysquid.mods.sodium.neoforge;

import me.jellysquid.mods.sodium.client.services.PlatformInfoAccess;
import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class NeoForgeInfoAccess implements PlatformInfoAccess {
    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public Path getGameDirectory() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isFlawlessFramesActive() {
        return false;
    }

    @Override
    public boolean platformHasEarlyLoadingScreen() {
        return FMLConfig.getBoolConfigValue(FMLConfig.ConfigValue.EARLY_WINDOW_CONTROL);
    }
}
