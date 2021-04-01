package net.smileycorp.deathchest;

import java.nio.ByteBuffer;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.BlockFluidFinite;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import org.apache.commons.lang3.ArrayUtils;

import com.mojang.authlib.GameProfile;

@EventBusSubscriber
@Mod(modid = "deathchest", acceptableRemoteVersions="*")
public class DeathChest {
	
	public static boolean hasSkull;
	public static boolean lockChest;
	public static boolean giveJournal;
	public static boolean journalPos;
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event){
		Config.config = new Configuration(event.getSuggestedConfigurationFile());
		Config.syncConfig();
	}
	
	@SubscribeEvent
	public static void onDeathEvent(LivingDeathEvent event) {
		if (event.getEntityLiving() instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer)event.getEntityLiving();
			World world = player.world;
			NonNullList<ItemStack> items = NonNullList.create();
			NonNullList<ItemStack> items2 = NonNullList.create();
			InventoryPlayer inventory = player.inventory;
			for (ItemStack stack : inventory.mainInventory) {
				addStack(stack, items, items2);
			}
			for (ItemStack stack : inventory.armorInventory) {
				addStack(stack, items, items2);
			}
			addStack(inventory.offHandInventory.get(0), items, items2);
			if (items.size()>0) {
				for (double i = player.getPosition().getY(); i < 255; i++) {
					BlockPos pos = new BlockPos(player.getPosition().getX(), i, player.getPosition().getZ());
					Block block = world.getBlockState(pos).getBlock();
					if (world.isAirBlock(pos)|| block instanceof BlockBush || block instanceof BlockFluidBase){
						world.setBlockState(pos, Blocks.CHEST.getDefaultState());

						
						NBTTagCompound nbt;
						
						TileEntityChest te = new TileEntityChest();
						
						te.setCustomName(player.getDisplayName().getUnformattedText()+"'s Loot");
						for (int slot = 0; slot<items.size(); slot++) {
							te.setInventorySlotContents(slot, items.get(slot));
						}
						if(lockChest) {
							nbt=te.writeToNBT(new NBTTagCompound());
							nbt.setString("Lock", "Death Journal "+getDeathValue(player, pos));
							te.readFromNBT(nbt);
							te.markDirty();
						}
						world.setTileEntity(pos, te);
						
						if (items2.size()>0) {
							pos = pos.up();
							block = world.getBlockState(pos).getBlock();
							world.setBlockState(pos, Blocks.CHEST.getDefaultState());
							te = new TileEntityChest();
							te.setCustomName(player.getDisplayName().getUnformattedText()+"'s Loot");
							for (int slot = 0; slot<items2.size(); slot++) {
								te.setInventorySlotContents(slot, items2.get(slot));
							}
							if(lockChest) {
								nbt=te.writeToNBT(new NBTTagCompound());
								nbt.setString("Lock", "Death Journal "+getDeathValue(player, pos));
								te.readFromNBT(nbt);
								te.markDirty();
							}
							world.setTileEntity(pos, te);
						}
						
						if (hasSkull) {
							if(world.isAirBlock(pos.up())) {
								world.setBlockState(pos.up(), Blocks.SKULL.getDefaultState());
								TileEntitySkull skull = new TileEntitySkull();
								skull.setPlayerProfile(new GameProfile((UUID)null, player.getName()));
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
		EntityPlayer player = event.getOriginal();
		if (player!=null&&event.isWasDeath()&&giveJournal) {
			BlockPos pos = player.getPosition();
			int dim = player.getEntityWorld().provider.getDimension();
			long time = player.getEntityWorld().getWorldTime();
			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setInteger("generation", 3);
			nbt.setString("title", "Death Journal");
			nbt.setString("author", player.getDisplayName().getUnformattedText());
			String contents = "{\"text\":\"Death Time: "+time+"\\n\\nDimension: "+dim+"\\n";
			if (journalPos) {
				contents += "\\nPosition: "+pos.getX()+", "+pos.getY()+", "+pos.getZ();
			}
			contents+="\"}";
			NBTTagList list = new NBTTagList();
			list.appendTag(new NBTTagString(contents));
			nbt.setTag("pages", list);
			ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
			stack.setTagCompound(nbt);
			if(lockChest) {
				stack.setStackDisplayName("Death Journal "+getDeathValue(player, pos));
			}
			if(!event.getEntityPlayer().inventory.addItemStackToInventory(stack)) {
				if(!event.getEntityPlayer().getEntityWorld().isRemote) {
					event.getEntityPlayer().entityDropItem(stack, 1F);
				}
			}
			event.getEntityPlayer().inventoryContainer.detectAndSendChanges();
			event.getEntityPlayer().inventory.markDirty();
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
	
	private static String getDeathValue(EntityPlayer player, BlockPos pos) {
		byte[] bytes = ByteBuffer.allocate(Integer.BYTES).putInt(pos.getX()).array();
		ArrayUtils.addAll(bytes, ByteBuffer.allocate(Integer.BYTES).putInt(pos.getY()).array());
		ArrayUtils.addAll(bytes, ByteBuffer.allocate(Integer.BYTES).putInt(pos.getZ()).array());
		ArrayUtils.addAll(bytes, EntityPlayer.getUUID(player.getGameProfile()).toString().getBytes());
		return new String(bytes);
	}
	
	static class Config {
		
		public static Configuration config;
	
		public static void syncConfig(){
			try{
				config.load();
				//general
				hasSkull = config.get(Configuration.CATEGORY_GENERAL, "hasSkull",  false, "whether a death chest spawns with a skull above it").getBoolean();
				giveJournal = config.get(Configuration.CATEGORY_GENERAL, "giveJournal", true, "whether players should be given a journal of their death").getBoolean();
				lockChest = config.get(Configuration.CATEGORY_GENERAL, "lockChest", true, "whether chests should be locked so that only its owner's journal can open it\nonly works if giveJournal is true").getBoolean();
				journalPos = config.get(Configuration.CATEGORY_GENERAL, "journalPos", true, "whether journal shows death position\nonly works if giveJournal is true").getBoolean();
				
				
			} catch (Exception e) {
			} finally {
				if (config.hasChanged()) config.save();
			}
		}
	
	}
}
