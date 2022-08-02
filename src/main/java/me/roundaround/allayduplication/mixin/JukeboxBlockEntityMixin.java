package me.roundaround.allayduplication.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.roundaround.allayduplication.compat.mixin.JukeboxBlockEntityExtended;
import net.minecraft.block.BlockState;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityMixin extends BlockEntity implements JukeboxBlockEntityExtended {
  public int ticksThisSecond;
  public long tickCount;
  public long recordStartTick;
  public boolean isPlaying;

  public JukeboxBlockEntityMixin(BlockPos pos, BlockState state) {
    super(BlockEntityType.JUKEBOX, pos, state);
  }

  @Inject(method = "readNbt", at = @At("TAIL"))
  public void readNbt(NbtCompound nbt, CallbackInfo info) {
    isPlaying = nbt.getBoolean("IsPlaying");
    recordStartTick = nbt.getLong("RecordStartTick");
    tickCount = nbt.getLong("TickCount");
  }

  @Inject(method = "writeNbt", at = @At("TAIL"))
  public void writeNbt(NbtCompound nbt, CallbackInfo info) {
    nbt.putBoolean("IsPlaying", isPlaying);
    nbt.putLong("RecordStartTick", recordStartTick);
    nbt.putLong("TickCount", tickCount);
  }

  @Inject(method = "clear", at = @At("TAIL"))
  public void clear(CallbackInfo info) {
    isPlaying = false;
  }

  @Override
  public void startPlaying() {
    recordStartTick = tickCount;
    isPlaying = true;
  }

  @Override
  public void incrementTicksThisSecond() {
    ticksThisSecond++;
  }

  @Override
  public void resetTicksThisSecond() {
    ticksThisSecond = 0;
    
  }

  @Override
  public void incrementTickCount() {
    tickCount++;
  }

  @Override
  public void setIsPlaying(boolean isPlaying) {
    this.isPlaying = isPlaying;
  }

  @Override
  public boolean isPlayingRecord(BlockState state) {
    return state.get(JukeboxBlock.HAS_RECORD) != false && isPlaying;
  }

  @Override
  public boolean isSongFinished(MusicDiscItem musicDisc) {
    return tickCount >= recordStartTick + getMusicDiscLength(musicDisc);
  }

  @Override
  public boolean hasSecondPassed() {
    return ticksThisSecond >= 20;
  }

  private static int getMusicDiscLength(MusicDiscItem musicDisc) {
    switch (musicDisc.getComparatorOutput()) {
      case 1:
        return 178;
      case 2:
        return 185;
      case 3:
        return 345;
      case 4:
        return 185;
      case 5:
        return 174;
      case 6:
        return 197;
      case 7:
        return 96;
      case 8:
        return 150;
      case 9:
        return 188;
      case 10:
        return 251;
      case 11:
        return 71;
      case 12:
        return 238;
      case 13:
        return 149;
      case 14:
        return 196;
      case 15:
      default:
        return 178;
    }
  }
}
