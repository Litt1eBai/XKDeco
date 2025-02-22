package org.teacon.xkdeco.blockentity;

import java.util.Objects;

import org.teacon.xkdeco.block.MimicWallBlock;
import org.teacon.xkdeco.init.XKDecoEntityTypes;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

@Deprecated
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class MimicWallBlockEntity extends BlockEntity {
	private Block eastBlock = Blocks.AIR;
	private Block westBlock = Blocks.AIR;
	private Block southBlock = Blocks.AIR;
	private Block northBlock = Blocks.AIR;

	public MimicWallBlockEntity(BlockPos pWorldPosition, BlockState pBlockState) {
		super(XKDecoEntityTypes.MIMIC_WALL.getOrCreate(), pWorldPosition, pBlockState);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag() {
		return this.saveWithoutMetadata();
	}

	public void updateBlocksFromLevel(MimicWallBlock wall) {
		var pos = this.getBlockPos();
		var block = this.getBlockState().getBlock();
		if (this.level != null) {
			var northWall = wall.connectsTo(this.level.getBlockState(pos.north()));
			var eastWall = wall.connectsTo(this.level.getBlockState(pos.east()));
			var southWall = wall.connectsTo(this.level.getBlockState(pos.south()));
			var westWall = wall.connectsTo(this.level.getBlockState(pos.west()));
			this.northBlock = northWall.orElse(Blocks.AIR);
			this.eastBlock = eastWall.orElse(Blocks.AIR);
			this.southBlock = southWall.orElse(Blocks.AIR);
			this.westBlock = westWall.orElse(Blocks.AIR);
			this.setChanged();
		}
	}

	@Override
	public void load(CompoundTag pTag) {
		super.load(pTag);
		this.northBlock = Objects.requireNonNullElse(
				ForgeRegistries.BLOCKS.getValue(new ResourceLocation(pTag.getString("NorthBlockName"))),
				Blocks.AIR);
		this.eastBlock = Objects.requireNonNullElse(
				ForgeRegistries.BLOCKS.getValue(new ResourceLocation(pTag.getString("EastBlockName"))),
				Blocks.AIR);
		this.southBlock = Objects.requireNonNullElse(
				ForgeRegistries.BLOCKS.getValue(new ResourceLocation(pTag.getString("SouthBlockName"))),
				Blocks.AIR);
		this.westBlock = Objects.requireNonNullElse(
				ForgeRegistries.BLOCKS.getValue(new ResourceLocation(pTag.getString("WestBlockName"))),
				Blocks.AIR);
		if (this.getBlockState().getBlock() instanceof MimicWallBlock wall) {
			this.updateBlocksFromLevel(wall);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag pTag) {
		super.saveAdditional(pTag);
		pTag.putString(
				"NorthBlockName",
				Objects.requireNonNullElse(ForgeRegistries.BLOCKS.getKey(this.northBlock), ForgeRegistries.BLOCKS.getKey(Blocks.AIR))
						.toString());
		pTag.putString(
				"EastBlockName",
				Objects.requireNonNullElse(ForgeRegistries.BLOCKS.getKey(this.eastBlock), ForgeRegistries.BLOCKS.getKey(Blocks.AIR))
						.toString());
		pTag.putString(
				"SouthBlockName",
				Objects.requireNonNullElse(ForgeRegistries.BLOCKS.getKey(this.southBlock), ForgeRegistries.BLOCKS.getKey(Blocks.AIR))
						.toString());
		pTag.putString(
				"WestBlockName",
				Objects.requireNonNullElse(ForgeRegistries.BLOCKS.getKey(this.westBlock), ForgeRegistries.BLOCKS.getKey(Blocks.AIR))
						.toString());
	}
}
