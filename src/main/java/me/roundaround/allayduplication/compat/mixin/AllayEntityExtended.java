package me.roundaround.allayduplication.compat.mixin;

public interface AllayEntityExtended {
  boolean isDancing();
  float getLerpedAnimationProgress(float f);
  boolean getDanceProgress();
  void startDuplicationCooldown();
}
