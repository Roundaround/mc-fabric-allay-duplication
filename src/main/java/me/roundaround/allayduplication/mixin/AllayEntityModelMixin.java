package me.roundaround.allayduplication.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.roundaround.allayduplication.compat.mixin.AllayEntityExtended;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.AllayEntityModel;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.util.math.MathHelper;

@Mixin(AllayEntityModel.class)
public abstract class AllayEntityModelMixin {
  @Shadow
  private ModelPart root;

  @Shadow
  private ModelPart field_39459;

  @Inject(method = "setAngles", at = @At("TAIL"))
  public void setAngles(
      AllayEntity allayEntity,
      float limbSwing,
      float limbSwingProgress,
      float partialTicks,
      float headYaw,
      float headPitch,
      CallbackInfo info) {
    AllayEntityExtended entity = (AllayEntityExtended) allayEntity;

    float time = partialTicks - allayEntity.age;

    if (entity.isDancing()) {
      float progress = entity.getLerpedAnimationProgress(time);
      float angle = partialTicks * 8f * ((float) Math.PI / 180) + limbSwingProgress;

      root.yaw = entity.getDanceProgress() ? (float) Math.PI * 4 * progress : root.yaw;
      root.roll = 16f * radToDeg(angle) * (1f - progress);
      field_39459.yaw = 30f * radToDeg(angle) * (1f - progress);
      field_39459.roll = 14f * radToDeg(angle) * (1f - progress);
    } else {
      field_39459.pitch = headPitch * ((float) Math.PI / 180);
      field_39459.yaw = headYaw * ((float) Math.PI / 180);
    }
  }

  private float radToDeg(float angle) {
    return MathHelper.cos(angle) * ((float) Math.PI / 180);
  }
}
