package net.smileycorp.deathchest;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod("deathchest")
public class DeathChest {

	public static Capability<DeathTracker> DEATH_TRACKER_CAPABILITY = CapabilityManager.get(new CapabilityToken<DeathTracker>(){});

	public DeathChest() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigHandler.config);
	}

}
