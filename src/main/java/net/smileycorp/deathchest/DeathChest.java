package net.smileycorp.deathchest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig;

@EventBusSubscriber
@Mod("deathchest")
public class DeathChest {
	
	public static BooleanValue hasSkull;
	public static BooleanValue lockChest;
	public static BooleanValue giveJournal;
	public static BooleanValue journalPos;
	
	public DeathChest() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.config);
	}
	
	@SubscribeEvent
	public static void onDeathEvent(LivingDeathEvent event) {
		Level world = event.getEntity().level;
		if (event.getEntityLiving() instanceof Player &! world.isClientSide) {
			Player player = (Player)event.getEntityLiving();
			NonNullList<ItemStack> items = NonNullList.create();
			NonNullList<ItemStack> items2 = NonNullList.create();
			Inventory inventory = player.getInventory();
			for (ItemStack stack : inventory.items) {
				addStack(stack, items, items2, 27);
			}
			for (ItemStack stack : inventory.armor) {
				addStack(stack, items, items2, 27);
			}
			addStack(inventory.offhand.get(0), items, items2, 27);
			if (items.size()>0) {
				for (double i = player.position().y; i < 255; i++) {
					BlockPos pos = new BlockPos(player.position());
					Block block = world.getBlockState(pos).getBlock();
					if (world.isEmptyBlock(pos)|| block instanceof BushBlock || block instanceof LiquidBlock){
						if (block == Blocks.WATER) {
							world.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.WATERLOGGED, true));
						} else {
							world.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
						}
						
						CompoundTag nbt;
						
						ChestBlockEntity te = new ChestBlockEntity(pos, Blocks.CHEST.defaultBlockState());
						
						te.setCustomName(new TextComponent(player.getDisplayName().getString()+"'s Loot"));
						for (int slot = 0; slot<items.size(); slot++) {
							te.setItem(slot, items.get(slot));
						}
						if(lockChest.get()) {
							nbt=te.save(new CompoundTag());
							nbt.putString("Lock", player.getStringUUID());
							te.load(nbt);
							te.setChanged();
						}
						world.setBlockEntity(te);
						
						if (items2.size()>0) {
							pos = pos.above();
							if (world.isEmptyBlock(pos)) {
								world.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
							} else {
								if (block == Blocks.WATER) {
									world.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.WATERLOGGED, true));
								} else {
									world.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
								}
							}
							te = new ChestBlockEntity(pos, Blocks.CHEST.defaultBlockState());
							te.setCustomName(new TextComponent(player.getDisplayName().getString()+"'s Loot"));
							for (int slot = 0; slot<items2.size(); slot++) {
								te.setItem(slot, items2.get(slot));
							}
							if(lockChest.get()) {
								nbt=te.save(new CompoundTag());
								nbt.putString("Lock", player.getStringUUID());
								te.load(nbt);
								te.setChanged();
							}
							world.setBlockEntity(te);
						}
						
						if (hasSkull.get()) {
							if(world.isEmptyBlock(pos.above())) {
								world.setBlockAndUpdate(pos.above(), Blocks.PLAYER_HEAD.defaultBlockState());
								SkullBlockEntity skull = new SkullBlockEntity(pos.above(), Blocks.PLAYER_HEAD.defaultBlockState());
								skull.setOwner(player.getGameProfile());
								world.setBlockEntity(skull);
							}
						}
						
						for (ServerPlayer splayer : world.getServer().getPlayerList().getPlayers()){
							splayer.sendMessage(event.getSource().getLocalizedDeathMessage(player), null);
						}
						event.setCanceled(true);
						break;
					}
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRespawnEvent(PlayerEvent.Clone event) {
		Player player = event.getOriginal();
		if (!player.level.isClientSide) {
			if (player!=null&&event.isWasDeath()&&giveJournal.get()) {
				BlockPos pos = player.blockPosition();
				long time = player.level.getGameTime();
				CompoundTag nbt = new CompoundTag();
				nbt.putInt("generation", 3);
				nbt.putString("title", "Death Journal");
				nbt.putString("author", player.getDisplayName().getString());
				String contents = "{\"text\":\"Death Time: "+time+"\\n\\nDimension: "+player.level.dimension().toString()+"\\n";
				if (journalPos.get()) {
					contents += "\\nPosition: "+pos.getX()+", "+pos.getY()+", "+pos.getZ();
				}
				contents+="\"}";
				ListTag list = new ListTag();
				list.add(StringTag.valueOf(contents));
				nbt.put("pages", list);
				ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
				if(lockChest.get()) {
					nbt.putString("Key", player.getStringUUID());
				}
				stack.setTag(nbt);
				if(!event.getPlayer().getInventory().add(stack)) {
					event.getPlayer().drop(stack, true);
				}
				event.getPlayer().containerMenu.sendAllDataToRemote();
				event.getPlayer().getInventory().setChanged();
			}
		}
	}
		
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void interactBlockEvent(RightClickBlock event) {
		Player player = event.getPlayer();
		Level world = player.level;
		if (!world.isClientSide) {
			BlockPos pos = event.getPos();
			if (world.getBlockEntity(pos) instanceof ChestBlockEntity) {
				ChestBlockEntity te = (ChestBlockEntity) world.getBlockEntity(pos);
				CompoundTag nbt = te.save(new CompoundTag());
				String lock = nbt.getString("Lock");
				if (lock != null) {
					ItemStack stack = player.getItemInHand(event.getHand());
					if (stack.getItem() == Items.WRITTEN_BOOK) {
						CompoundTag stackNBT = stack.getTag();
						int gen = stackNBT.getInt("generation");
						if (gen == 3) {
							String key = stackNBT.getString("Key");
							if (key != null && key.equals(lock)) {
								nbt.remove("Lock");
								te.load(nbt);
								te.setChanged();
								player.sendMessage(new TextComponent("Chest has been unlocked."), null);
							}
						}
					}
				}
			}
		}
	}

	private static void addStack(ItemStack stack, NonNullList<ItemStack> items,
			NonNullList<ItemStack> items2, int size) {
		if (!stack.isEmpty()) {
			if (items.size()==size){
				items2.add(stack);
			}else {
				items.add(stack);
			}
		}
	}
	
	static class Config {
		
		public static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		public static final ForgeConfigSpec config;
	
		 static {
			builder.push("general");
			hasSkull = builder.comment("whether a death chest spawns with a skull above it\n")
						.define("hasSkull", false);
			giveJournal = builder.comment("whether players should be given a journal of their death\n")
						.define("giveJournal", true);
			lockChest = builder.comment("whether chests should be locked so that only its owner's journal can open it\nonly works if giveJournal is true\n")
						.define("lockChest", true);
			journalPos = builder.comment("whether journal shows death position\nonly works if giveJournal is true\n")
						.define("journalPos", true);
			
			builder.pop();
			config = builder.build();
		}
	
	}
}
