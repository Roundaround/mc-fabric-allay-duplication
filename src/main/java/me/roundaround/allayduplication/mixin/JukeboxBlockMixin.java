package me.roundaround.allayduplication.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.roundaround.allayduplication.AllayDuplicationMod;
import me.roundaround.allayduplication.compat.mixin.JukeboxBlockEntityExtended;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;

@Mixin(JukeboxBlock.class)
public abstract class JukeboxBlockMixin extends BlockWithEntity {
  public JukeboxBlockMixin(AbstractBlock.Settings settings) {
    super(settings);
    this.setDefaultState(stateManager.getDefaultState().with(JukeboxBlock.HAS_RECORD, false));
  }

  @Inject(method = "setRecord", at = @At(value = "INVOKE", target = "net/minecraft/block/entity/JukeboxBlockEntity.setRecord(Lnet/minecraft/item/ItemStack;)V", shift = At.Shift.AFTER))
  public void setRecord(
      Entity user,
      WorldAccess world,
      BlockPos pos,
      BlockState state,
      ItemStack stack,
      CallbackInfo info) {
    JukeboxBlockEntityExtended blockEntity = (JukeboxBlockEntityExtended) world.getBlockEntity(pos);
    blockEntity.startPlaying();
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      World world,
      BlockState state,
      BlockEntityType<T> type) {
    if (state.get(JukeboxBlock.HAS_RECORD).booleanValue()) {
      return JukeboxBlock.checkType(type, BlockEntityType.JUKEBOX, JukeboxBlockMixin::tickJukebox);
    }
    return null;
  }

  private static void tickJukebox(World world, BlockPos pos, BlockState state, JukeboxBlockEntity blockEntity) {
    JukeboxBlockEntityExtended entity = (JukeboxBlockEntityExtended) blockEntity;
    Item item;
    entity.incrementTicksThisSecond();

    if (entity.isPlayingRecord(state) && (item = blockEntity.getRecord().getItem()) instanceof MusicDiscItem) {
      MusicDiscItem musicDiscItem = (MusicDiscItem) item;
      if (entity.isSongFinished(musicDiscItem)) {
        world.emitGameEvent(AllayDuplicationMod.JUKEBOX_STOP_PLAY, pos, GameEvent.Emitter.of(state));
        entity.setIsPlaying(false);
      } else if (entity.hasSecondPassed()) {
        entity.resetTicksThisSecond();
        world.emitGameEvent(AllayDuplicationMod.JUKEBOX_PLAY, pos, GameEvent.Emitter.of(state));
      }
    }

    entity.incrementTickCount();
  }
}
