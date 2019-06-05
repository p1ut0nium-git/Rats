package com.github.alexthe666.rats.server.entity.ai;

import com.github.alexthe666.rats.server.entity.EntityRat;
import com.github.alexthe666.rats.server.entity.RatUtils;
import com.github.alexthe666.rats.server.entity.tile.TileEntityRatTrap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RatAIEnterTrap extends EntityAIBase {
    private static final int RADIUS = 16;

    private BlockPos targetBlock = null;
    private final EntityRat entity;
    private final BlockSorter targetSorter;
    private int feedingTicks;

    public RatAIEnterTrap(EntityRat entity) {
        super();
        this.entity = entity;
        this.targetSorter = new BlockSorter(entity);
        this.setMutexBits(0);
    }

    @Override
    public boolean shouldExecute() {
        if (!this.entity.canMove() || this.entity.isTamed() || this.entity.isInCage()) {
            return false;
        }
        if (!this.entity.getHeldItem(EnumHand.MAIN_HAND).isEmpty()) {
            return false;
        }
        if (this.entity.isTamed()) {
            return false;
        }
        resetTarget();
        return targetBlock != null;
    }

    private void resetTarget() {
        List<BlockPos> allBlocks = new ArrayList<>();
        for (BlockPos pos : BlockPos.getAllInBox(this.entity.getPosition().add(-RADIUS, -RADIUS, -RADIUS), this.entity.getPosition().add(RADIUS, RADIUS, RADIUS))) {
            TileEntity entity = this.entity.world.getTileEntity(pos);
            if (entity instanceof TileEntityRatTrap) {
                if (!((TileEntityRatTrap) entity).isShut && !((TileEntityRatTrap) entity).getBait().isEmpty()) {
                    allBlocks.add(pos);
                }
            }
        }
        if (!allBlocks.isEmpty()) {
            allBlocks.sort(this.targetSorter);
            this.targetBlock = allBlocks.get(0);
        }
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (targetBlock != null) {
            TileEntity te = this.entity.world.getTileEntity(targetBlock);
            return this.entity.world.getTileEntity(targetBlock) != null && this.entity.world.getTileEntity(targetBlock) instanceof TileEntityRatTrap && !((TileEntityRatTrap) te).isShut && !((TileEntityRatTrap) te).getBait().isEmpty();
        }
        return false;
    }

    public void resetTask() {
        this.entity.getNavigator().clearPath();
        resetTarget();
    }

    public boolean canSeeChest() {
        RayTraceResult rayTrace = RatUtils.rayTraceBlocksIgnoreRatholes(entity.world, entity.getPositionVector(), new Vec3d(targetBlock.getX() + 0.5, targetBlock.getY() + 0.5, targetBlock.getZ() + 0.5), false);
        if (rayTrace != null && rayTrace.hitVec != null) {
            BlockPos sidePos = rayTrace.getBlockPos();
            BlockPos pos = new BlockPos(rayTrace.hitVec);
            return entity.world.isAirBlock(sidePos) || entity.world.isAirBlock(pos) || this.entity.world.getTileEntity(pos) == this.entity.world.getTileEntity(targetBlock);
        }
        return true;
    }

    @Override
    public void updateTask() {
        if (this.targetBlock != null) {
            TileEntity entity = this.entity.world.getTileEntity(this.targetBlock);
            this.entity.getNavigator().tryMoveToXYZ(this.targetBlock.getX() + 0.5D, this.targetBlock.getY(), this.targetBlock.getZ() + 0.5D, 1D);
            if (entity instanceof TileEntityRatTrap && !((TileEntityRatTrap) entity).isShut && !((TileEntityRatTrap) entity).getBait().isEmpty()) {
                double distance = this.entity.getDistance(this.targetBlock.getX(), this.targetBlock.getY(), this.targetBlock.getZ());
                if (distance < 0.5F && canSeeChest()) {
                    ItemStack duplicate = ((TileEntityRatTrap) entity).getBait().copy();
                    duplicate.setCount(1);
                    this.entity.setHeldItem(EnumHand.MAIN_HAND, duplicate);
                    ((TileEntityRatTrap) entity).getBait().shrink(1);
                    this.targetBlock = null;
                    this.resetTask();
                    this.entity.fleePos = this.targetBlock;
                }
            }

        }
    }

    public class BlockSorter implements Comparator<BlockPos> {
        private final Entity entity;

        public BlockSorter(Entity entity) {
            this.entity = entity;
        }

        @Override
        public int compare(BlockPos pos1, BlockPos pos2) {
            double distance1 = this.getDistance(pos1);
            double distance2 = this.getDistance(pos2);
            return Double.compare(distance1, distance2);
        }

        private double getDistance(BlockPos pos) {
            double deltaX = this.entity.posX - (pos.getX() + 0.5);
            double deltaY = this.entity.posY + this.entity.getEyeHeight() - (pos.getY() + 0.5);
            double deltaZ = this.entity.posZ - (pos.getZ() + 0.5);
            return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
        }
    }
}
