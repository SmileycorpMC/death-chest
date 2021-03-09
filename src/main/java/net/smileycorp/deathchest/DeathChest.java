package net.smileycorp.deathchest;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.commons.lang3.ArrayUtils;

import com.mojang.authlib.GameProfile;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BushBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.SkullTileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
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
		if (event.getEntityLiving() instanceof PlayerEntity) {
			PlayerEntity player = (PlayerEntity)event.getEntityLiving();
			World world = player.world;
			NonNullList<ItemStack> items = NonNullList.create();
			NonNullList<ItemStack> items2 = NonNullList.create();
			PlayerInventory inventory = player.inventory;
			for (ItemStack stack : inventory.mainInventory) {
				addStack(stack, items, items2);
			}
			for (ItemStack stack : inventory.armorInventory) {
				addStack(stack, items, items2);
			}
			addStack(inventory.offHandInventory.get(0), items, items2);
			if (items.size()>0) {
				for (double i = player.getPosY(); i < 255; i++) {
					BlockPos pos = new BlockPos(player.getPosX(), i, player.getPosZ());
					Block block = world.getBlockState(pos).getBlock();
					if (world.isAirBlock(pos)|| block instanceof BushBlock || block instanceof FlowingFluidBlock){
						if (block == Blocks.WATER) {
							world.setBlockState(pos, Blocks.CHEST.getDefaultState().with(ChestBlock.WATERLOGGED, true));
						} else {
							world.setBlockState(pos, Blocks.CHEST.getDefaultState());
						}
						
						CompoundNBT nbt;
						
						ChestTileEntity te = new ChestTileEntity();
						
						te.setCustomName(new StringTextComponent(player.getDisplayName().getString()+"'s Loot"));
						for (int slot = 0; slot<items.size(); slot++) {
							te.setInventorySlotContents(slot, items.get(slot));
						}
						if(lockChest.get()) {
							nbt=te.write(new CompoundNBT());
							nbt.putString("Lock", "Death Journal "+getDeathValue(player, pos));
							te.read(world.getBlockState(pos), nbt);
							te.markDirty();
						}
						world.setTileEntity(pos, te);
						
						if (items2.size()>0) {
							pos = pos.up();
							block = world.getBlockState(pos).getBlock();
							if (block == Blocks.WATER) {
								world.setBlockState(pos, Blocks.CHEST.getDefaultState().with(ChestBlock.WATERLOGGED, true));
							} else {
								world.setBlockState(pos, Blocks.CHEST.getDefaultState());
							}
							te = new ChestTileEntity();
							te.setCustomName(new StringTextComponent(player.getDisplayName().getString()+"'s Loot"));
							for (int slot = 0; slot<items2.size(); slot++) {
								te.setInventorySlotContents(slot, items2.get(slot));
							}
							if(lockChest.get()) {
								nbt=te.write(new CompoundNBT());
								nbt.putString("Lock", "Death Journal "+getDeathValue(player, pos));
								te.read(world.getBlockState(pos), nbt);
								te.markDirty();
							}
							world.setTileEntity(pos, te);
						}
						
						if (hasSkull.get()) {
							if(world.isAirBlock(pos.up())) {
								world.setBlockState(pos.up(), Blocks.PLAYER_HEAD.getDefaultState());
								SkullTileEntity skull = new SkullTileEntity();
								skull.setPlayerProfile(new GameProfile((UUID)null, player.getName().getString()));
								world.setTileEntity(pos.up(), skull);
							}
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
		PlayerEntity player = event.getOriginal();
		if (player!=null&&event.isWasDeath()&&giveJournal.get()) {
			BlockPos pos = player.getPosition();
			long time = player.getEntityWorld().getGameTime();
			CompoundNBT nbt = new CompoundNBT();
			nbt.putInt("generation", 3);
			nbt.putString("title", "Death Journal");
			nbt.putString("author", player.getDisplayName().getString());
			String contents = "{\"text\":\"Death Time: "+time+"\\n\\nDimension: "+player.getEntityWorld().getDimensionKey().getLocation()+"\\n";
			if (journalPos.get()) {
				contents += "\\nPosition: "+pos.getX()+", "+pos.getY()+", "+pos.getZ();
			}
			contents+="\"}";
			ListNBT list = new ListNBT();
			list.add(StringNBT.valueOf(contents));
			nbt.put("pages", list);
			ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
			stack.setTag(nbt);
			if(lockChest.get()) {
				stack.setDisplayName(new StringTextComponent("Death Journal "+getDeathValue(player, pos)));
			}
			if(!event.getPlayer().inventory.addItemStackToInventory(stack)) {
				if(!event.getPlayer().getEntityWorld().isRemote) {
					event.getPlayer().entityDropItem(stack, 1F);
				}
			}
			event.getPlayer().container.detectAndSendChanges();
			event.getPlayer().inventory.markDirty();
		}
	}

	private static void addStack(ItemStack stack, NonNullList<ItemStack> items,
			NonNullList<ItemStack> items2) {
		if (!stack.isEmpty()) {
			if (items.size()==27){
				items2.add(stack);
			}else {
				items.add(stack);
			}
		}
	}
	
	private static String getDeathValue(PlayerEntity player, BlockPos pos) {
		byte[] bytes = ByteBuffer.allocate(Integer.BYTES).putInt(pos.getX()).array();
		ArrayUtils.addAll(bytes, ByteBuffer.allocate(Integer.BYTES).putInt(pos.getY()).array());
		ArrayUtils.addAll(bytes, ByteBuffer.allocate(Integer.BYTES).putInt(pos.getZ()).array());
		ArrayUtils.addAll(bytes, PlayerEntity.getUUID(player.getGameProfile()).toString().getBytes());
		return new String(bytes);
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
