package com.breakinblocks.neosync.common.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.energy.IEnergyStorage;
import com.breakinblocks.neosync.api.event.PlayerSyncEvents;
import com.breakinblocks.neosync.api.shell.ShellState;
import com.breakinblocks.neosync.common.block.ShellConstructorBlock;
import com.breakinblocks.neosync.common.config.SyncConfig;
import com.breakinblocks.neosync.common.entity.damage.FingerstickDamageSource;
import com.breakinblocks.neosync.common.utils.BlockPosUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;

public class ShellConstructorBlockEntity extends AbstractShellContainerBlockEntity implements IEnergyStorage, IFluidHandler {
    public final FluidTank fluidTank = new FluidTank(SyncConfig.getInstance().shellConstructorFluidCapacity(), fluidStack -> {
        if (fluidStack == null || fluidStack.isEmpty()) return false;
        String fluidName = BuiltInRegistries.FLUID.getKey(fluidStack.getFluid()).toString();
        return SyncConfig.getInstance().shellConstructorFluidTypes().contains(fluidName);
    }) {
        @Override
        public int getCapacity() {
            return SyncConfig.getInstance().shellConstructorFluidCapacity();
        }
    };
    public ShellConstructorBlockEntity(BlockPos pos, BlockState state) {
        this(SyncBlockEntities.SHELL_CONSTRUCTOR.get(), pos, state);
    }

    protected ShellConstructorBlockEntity(BlockEntityType<? extends ShellConstructorBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onServerTick(Level world, BlockPos pos, BlockState state) {
        super.onServerTick(world, pos, state);
        if (ShellConstructorBlock.isOpen(state)) {
            ShellConstructorBlock.setOpen(state, world, pos, BlockPosUtil.hasPlayerInside(pos, world));
        }
    }

    public InteractionResult onUse(Level world, BlockPos pos, Player player, InteractionHand hand) {
        PlayerSyncEvents.ShellConstructionFailureReason failureReason = this.beginShellConstruction(player);
        if (failureReason == null) {
            return InteractionResult.SUCCESS;
        } else {
            player.displayClientMessage(failureReason.toText(), true);
            return InteractionResult.CONSUME;
        }
    }

    @Nullable
    private PlayerSyncEvents.ShellConstructionFailureReason beginShellConstruction(Player player) {
        PlayerSyncEvents.ShellConstructionFailureReason failureReason = this.shell == null
                ? PlayerSyncEvents.ALLOW_SHELL_CONSTRUCTION.invoker().allowShellConstruction(player, this)
                : PlayerSyncEvents.ShellConstructionFailureReason.OCCUPIED;

        if (failureReason != null) {
            return failureReason;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            SyncConfig config = SyncConfig.getInstance();

            float damage = serverPlayer.server.isHardcore() ? config.hardcoreFingerstickDamage() : config.fingerstickDamage();

            boolean isCreative = !serverPlayer.gameMode.isSurvival();
            boolean isLowOnHealth = (player.getHealth() + player.getAbsorptionAmount()) <= damage;
            boolean hasTotemOfUndying = player.getMainHandItem().is(Items.TOTEM_OF_UNDYING) || player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
            if (isLowOnHealth && !isCreative && !hasTotemOfUndying && config.warnPlayerInsteadOfKilling()) {
                return PlayerSyncEvents.ShellConstructionFailureReason.NOT_ENOUGH_HEALTH;
            }

            player.hurt(FingerstickDamageSource.fingerstick(player), damage);
            this.shell = ShellState.empty(serverPlayer, this.worldPosition);
            this.shell.bindTo(this);
            if (isCreative && config.enableInstantShellConstruction()) {
                this.shell.setProgress(ShellState.PROGRESS_DONE);
            }
            this.setChanged();
            this.sync();
        }
        return null;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        ShellConstructorBlockEntity bottom = (ShellConstructorBlockEntity) this.getBottomPart().orElse(null);
        if (bottom == null || bottom.shell == null || bottom.shell.getProgress() >= ShellState.PROGRESS_DONE) {
            return 0;
        }

        int capacity = (int) SyncConfig.getInstance().shellConstructorEnergyRequirement();
        int missingFE = (int) Math.ceil((ShellState.PROGRESS_DONE - bottom.shell.getProgress()) * capacity);
        int accepted = Math.min(maxReceive, missingFE);

        if (accepted > 0 && SyncConfig.getInstance().enableFluidConsumption()) {
            int requiredFluidTotal = Math.max(1, SyncConfig.getInstance().shellConstructorFluidAmount());
            int energyPerMb = Math.max(1, (int) Math.ceil((double) capacity / requiredFluidTotal));

            int availableMb = bottom.fluidTank.getFluidAmount();
            int maxFeByFluid = availableMb * energyPerMb;

            accepted = Math.min(accepted, maxFeByFluid);

            if (accepted > 0 && accepted == missingFE && missingFE < energyPerMb) {
                if (availableMb > 0) {
                    if (!simulate) bottom.fluidTank.drain(1, IFluidHandler.FluidAction.EXECUTE);
                } else {
                    accepted = 0;
                }
            } else {
                accepted = (accepted / energyPerMb) * energyPerMb;
                if (accepted > 0 && !simulate) {
                    bottom.fluidTank.drain(accepted / energyPerMb, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }

        if (accepted > 0 && !simulate) {
            bottom.shell.setProgress(bottom.shell.getProgress() + (float) accepted / capacity);
            bottom.setChanged();
            bottom.sync();
        }

        return accepted;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        ShellConstructorBlockEntity bottom = (ShellConstructorBlockEntity) this.getBottomPart().orElse(null);
        if (bottom == null || bottom.shell == null) {
            return 0;
        }
        int cap = (int) SyncConfig.getInstance().shellConstructorEnergyRequirement();
        return (int) (bottom.shell.getProgress() * cap);
    }

    @Override
    public int getMaxEnergyStored() {
        ShellConstructorBlockEntity bottom = (ShellConstructorBlockEntity) this.getBottomPart().orElse(null);
        return bottom != null && bottom.shell != null
                ? (int) SyncConfig.getInstance().shellConstructorEnergyRequirement()
                : 0;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        nbt.put("Fluid", this.fluidTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        if (nbt.contains("Fluid")) {
            this.fluidTank.readFromNBT(registries, nbt.getCompound("Fluid"));
        }
    }

    @Override
    public int getTanks() {
        if (!SyncConfig.getInstance().enableFluidConsumption()) return 0;
        return this.getBottomPart().map(b -> ((ShellConstructorBlockEntity) b).fluidTank.getTanks()).orElse(0);
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return this.getBottomPart().map(b -> ((ShellConstructorBlockEntity) b).fluidTank.getFluidInTank(tank)).orElse(FluidStack.EMPTY);
    }

    @Override
    public int getTankCapacity(int tank) {
        if (!SyncConfig.getInstance().enableFluidConsumption()) return 0;
        return this.getBottomPart().map(b -> ((ShellConstructorBlockEntity) b).fluidTank.getTankCapacity(tank)).orElse(0);
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return this.getBottomPart().map(b -> ((ShellConstructorBlockEntity) b).fluidTank.isFluidValid(tank, stack)).orElse(false);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return this.getBottomPart().map(b -> {
            ShellConstructorBlockEntity bottom = (ShellConstructorBlockEntity) b;
            int filled = bottom.fluidTank.fill(resource, action);
            if (filled > 0 && action.execute()) {
                bottom.setChanged();
                bottom.sync();
            }
            return filled;
        }).orElse(0);
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        return this.getBottomPart().map(b -> {
            ShellConstructorBlockEntity bottom = (ShellConstructorBlockEntity) b;
            FluidStack drained = bottom.fluidTank.drain(resource, action);
            if (!drained.isEmpty() && action.execute()) {
                bottom.setChanged();
                bottom.sync();
            }
            return drained;
        }).orElse(FluidStack.EMPTY);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        return this.getBottomPart().map(b -> {
            ShellConstructorBlockEntity bottom = (ShellConstructorBlockEntity) b;
            FluidStack drained = bottom.fluidTank.drain(maxDrain, action);
            if (!drained.isEmpty() && action.execute()) {
                bottom.setChanged();
                bottom.sync();
            }
            return drained;
        }).orElse(FluidStack.EMPTY);
    }
}