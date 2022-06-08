package net.smileycorp.deathchest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class EventListener {

	@SubscribeEvent
	public static void onDeathEvent(LivingDropsEvent event) {
		Level level = event.getEntity().level;
		if (event.getEntityLiving() instanceof Player &! level.isClientSide) {
			Player player = (Player)event.getEntityLiving();
			List<NonNullList<ItemStack>> items = new ArrayList<NonNullList<ItemStack>>();
			for (ItemEntity item : event.getDrops()) {
				if (items.isEmpty()) items.add(NonNullList.create());
				if (items.get(items.size()-1).size() >= 27) items.add(NonNullList.create());
				items.get(items.size()-1).add(item.getItem());
			}
			if (items.size()>0) {
				for (int i = (int) Math.floor(player.position().y); i < level.getMaxBuildHeight() - items.size(); i++) {
					BlockPos pos = new BlockPos(player.position());
					if (!canPlace(level, pos, items.size())) continue;
					for (int j = 0; j < items.size(); j++) {
						setChest(level, pos.above(j), player, items.get(j));
					}
					if (ConfigHandler.hasSkull.get()) {
						if(level.isEmptyBlock(pos.above(items.size()))) {
							level.setBlockAndUpdate(pos.above(items.size()), Blocks.PLAYER_HEAD.defaultBlockState());
							SkullBlockEntity skull = new SkullBlockEntity(pos.above(items.size()), Blocks.PLAYER_HEAD.defaultBlockState());
							skull.setOwner(player.getGameProfile());
							level.setBlockEntity(skull);
						}
					}
					event.setCanceled(true);
					return;
				}
			}
		}
	}


	private static boolean canPlace(Level level, BlockPos pos, int size) {
		for (int i = 0; i < size; i++) {
			Block block = level.getBlockState(pos.above(i)).getBlock();
			if (!(level.isEmptyBlock(pos)|| block instanceof BushBlock || block instanceof LiquidBlock)) return false;
		}
		return true;
	}


	private static void setChest(Level level, BlockPos pos, Player player, NonNullList<ItemStack> items) {
		if (level.getBlockState(pos).getBlock() == Blocks.WATER) {
			level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.WATERLOGGED, true));
		} else {
			level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
		}

		ChestBlockEntity te = new ChestBlockEntity(pos, Blocks.CHEST.defaultBlockState());

		if(ConfigHandler.lockChest.get()) {
			CompoundTag nbt=te.saveWithoutMetadata();
			nbt.putString("Lock", player.getStringUUID());
			te.load(nbt);
			te.setChanged();
		}

		te.setCustomName(MutableComponent.create(new TranslatableContents(player.getDisplayName().getString()+"'s Loot")));
		for (int slot = 0; slot<items.size(); slot++) te.setItem(slot, items.get(slot));
		level.setBlockEntity(te);

	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRespawnEvent(PlayerEvent.Clone event) {
		Player player = event.getOriginal();
		if (!player.level.isClientSide) {
			if (player!=null && event.isWasDeath() && ConfigHandler.giveJournal.get()) {
				BlockPos pos = player.blockPosition();
				long time = player.level.getGameTime();
				CompoundTag nbt = new CompoundTag();
				nbt.putInt("generation", 3);
				nbt.putString("title", "Death Journal");
				nbt.putString("author", player.getDisplayName().getString());
				StringBuilder contents = new StringBuilder("{\"text\":\"Death Time: "+time);
				contents.append("\\n\\nDimension: "+player.level.dimension().location()+"\\n");
				if (ConfigHandler.journalPos.get()) contents.append("\\nPosition: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
				contents.append("\\n\\nLocked: "+ ConfigHandler.lockChest.get());
				contents.append("\"}");
				ListTag list = new ListTag();
				list.add(StringTag.valueOf(contents.toString()));
				nbt.put("pages", list);
				ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
				if (ConfigHandler.lockChest.get()) nbt.putString("Key", player.getStringUUID());
				stack.setTag(nbt);
				if (!event.getPlayer().getInventory().add(stack)) event.getPlayer().drop(stack, true);
				event.getPlayer().containerMenu.sendAllDataToRemote();
				event.getPlayer().getInventory().setChanged();
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void interactBlockEvent(RightClickBlock event) {
		Player player = event.getPlayer();
		Level level = player.level;
		if (!level.isClientSide) {
			BlockPos pos = event.getPos();
			if (level.getBlockEntity(pos) instanceof ChestBlockEntity) {
				ChestBlockEntity te = (ChestBlockEntity) level.getBlockEntity(pos);
				CompoundTag nbt = te.saveWithoutMetadata();
				String lock = nbt.getString("Lock");
				if (lock != null) {
					if (player.getStringUUID().equals(lock)) {
						nbt.remove("Lock");
						te.load(nbt);
						te.setChanged();
						player.displayClientMessage(MutableComponent.create(new TranslatableContents("Chest has been unlocked.")), true);
						return;
					}
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
								player.displayClientMessage(MutableComponent.create(new TranslatableContents("Chest has been unlocked.")), true);
								return;
							}
						}
					}
				}
			}
		}
	}

}
