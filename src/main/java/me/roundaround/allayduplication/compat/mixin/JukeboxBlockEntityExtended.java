package me.roundaround.allayduplication.compat.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.item.MusicDiscItem;

public interface JukeboxBlockEntityExtended {
  void incrementTicksThisSecond();
  void resetTicksThisSecond();
  void incrementTickCount();
  void startPlaying();
  void setIsPlaying(boolean isPlaying);
  boolean isPlayingRecord(BlockState state);
  boolean isSongFinished(MusicDiscItem musicDisc);
  boolean hasSecondPassed();
}
