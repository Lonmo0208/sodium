package me.jellysquid.mods.sodium.neoforge.mixin;

import me.jellysquid.mods.sodium.client.services.SodiumModelData;
import net.minecraftforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ModelData.class)
public class ModelDataMixin implements SodiumModelData {
}
