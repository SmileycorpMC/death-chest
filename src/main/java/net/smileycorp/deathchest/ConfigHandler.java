package net.smileycorp.deathchest;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;

public class ConfigHandler {

	public static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
	public static final ForgeConfigSpec config;

	public static BooleanValue hasSkull;
	public static BooleanValue lockChest;
	public static BooleanValue giveJournal;
	public static BooleanValue journalPos;

	static {
		builder.push("general");
		ConfigHandler.hasSkull = builder.comment("whether a death chest spawns with a skull above it\n")
				.define("hasSkull", false);
		ConfigHandler.giveJournal = builder.comment("whether players should be given a journal of their death\n")
				.define("giveJournal", true);
		ConfigHandler.lockChest = builder.comment("whether chests should be locked so that only its owner can open it")
				.define("lockChest", true);
		ConfigHandler.journalPos = builder.comment("whether journal shows death position\nonly works if giveJournal is true\n")
				.define("journalPos", true);

		builder.pop();
		config = builder.build();
	}

}