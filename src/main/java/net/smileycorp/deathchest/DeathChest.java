package net.smileycorp.deathchest;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod("deathchest")
public class DeathChest {

	public DeathChest() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigHandler.config);
	}

}
