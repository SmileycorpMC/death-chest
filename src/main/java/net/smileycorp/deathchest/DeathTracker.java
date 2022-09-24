package net.smileycorp.deathchest;

import java.util.List;

import org.apache.commons.compress.utils.Lists;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public interface DeathTracker {

	public BlockPos getDeathPoint();

	public List<NonNullList<ItemStack>> getItems();

	public void setDeathPoint(BlockPos pos, List<ItemEntity> items);

	public void loadNBT(CompoundTag tag);

	public CompoundTag saveNBT();

	public static class Implementation implements DeathTracker {

		private final List<NonNullList<ItemStack>> items = Lists.newArrayList();
		private BlockPos pos;

		@Override
		public BlockPos getDeathPoint() {
			return pos;
		}

		@Override
		public List<NonNullList<ItemStack>> getItems() {
			return items;
		}

		@Override
		public void setDeathPoint(BlockPos pos, List<ItemEntity> items) {
			items.clear();
			this.pos = pos;
			for (ItemEntity item : items) {
				if (this.items.isEmpty()) this.items.add(NonNullList.create());
				if (this.items.get(this.items.size()-1).size() >= 27) this.items.add(NonNullList.create());
				this.items.get(this.items.size()-1).add(item.getItem());
			}
		}

		@Override
		public void loadNBT(CompoundTag tag) {
			if (tag.contains("pos")) {
				CompoundTag posTag = tag.getCompound("pos");
				pos = new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z"));
			}
			if (tag.contains("items")) {
				for (Tag inv : tag.getList("items", 9)) {
					NonNullList<ItemStack> stacks = NonNullList.create();
					for (Tag item : (ListTag)inv) {
						stacks.add(ItemStack.of((CompoundTag) item));
					}
				}
			}
		}

		@Override
		public CompoundTag saveNBT() {
			CompoundTag tag = new CompoundTag();
			if (pos != null) {
				CompoundTag posTag = new CompoundTag();
				posTag.putInt("x", pos.getX());
				posTag.putInt("y", pos.getY());
				posTag.putInt("z", pos.getZ());
				tag.put("pos", posTag);
			}
			ListTag itemsTag = new ListTag();
			for (NonNullList<ItemStack> stacks : items) {
				ListTag stacksTag = new ListTag();
				for (ItemStack stack : stacks) {
					stacksTag.add(stack.save(new CompoundTag()));
				}
				itemsTag.add(stacksTag);
			}
			return tag;
		}

	}

	public static class Provider implements ICapabilitySerializable<CompoundTag> {

		private final DeathTracker impl;

		public Provider() {
			impl = new Implementation();
		}

		@Override
		public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
			return cap == DeathChest.DEATH_TRACKER_CAPABILITY ? LazyOptional.of(() -> impl).cast() : LazyOptional.empty();
		}


		@Override
		public CompoundTag serializeNBT() {
			return impl.saveNBT();
		}

		@Override
		public void deserializeNBT(CompoundTag nbt) {
			impl.loadNBT(nbt);
		}

	}

}
