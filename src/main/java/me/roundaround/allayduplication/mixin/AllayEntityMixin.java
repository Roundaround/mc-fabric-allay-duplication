package me.roundaround.allayduplication.mixin;

import java.util.function.BiConsumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.roundaround.allayduplication.AllayDuplicationMod;
import me.roundaround.allayduplication.compat.mixin.AllayEntityExtended;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.event.EntityPositionSource;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.listener.EntityGameEventHandler;
import net.minecraft.world.event.listener.GameEventListener;

@Mixin(AllayEntity.class)
public abstract class AllayEntityMixin extends MobEntity implements AllayEntityExtended {
  private static final Ingredient DUPLICATION_INGREDIENT = Ingredient.ofItems(Items.AMETHYST_SHARD);
  private static final int DUPLICATION_COOLDOWN = 6000;
  private static final TrackedData<Boolean> DANCING = DataTracker.registerData(AllayEntity.class,
      TrackedDataHandlerRegistry.BOOLEAN);
  private static final TrackedData<Boolean> CAN_DUPLICATE = DataTracker.registerData(AllayEntity.class,
      TrackedDataHandlerRegistry.BOOLEAN);

  private EntityGameEventHandler<JukeboxEventListener> jukeboxEventHandler;
  private BlockPos jukeboxPos;
  private long duplicationCooldown;
  private float timeDanced;
  private float nextAnimFrame;
  private float prevAnimFrame;

  public AllayEntityMixin(EntityType<? extends AllayEntity> entityType, World world) {
    super(entityType, world);
  }

  @Inject(method = "<init>", at = @At("TAIL"))
  public void constructor(EntityType<? extends AllayEntity> entityType, World world, CallbackInfo info) {
    EntityPositionSource positionSource = new EntityPositionSource(this, this.getStandingEyeHeight());
    jukeboxEventHandler = new EntityGameEventHandler<JukeboxEventListener>(
        new JukeboxEventListener(positionSource, AllayDuplicationMod.JUKEBOX_PLAY.getRange()));
  }

  @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
  public void interactMob(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> info) {
    ItemStack itemStack = player.getStackInHand(hand);
    if (isDancing() && matchesDuplicationIngredient(itemStack) && canDuplicate()) {
      duplicate();
      world.sendEntityStatus(this, EntityStatuses.ADD_BREEDING_PARTICLES);
      world.playSoundFromEntity(
          player,
          this,
          SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
          SoundCategory.NEUTRAL,
          2f,
          1f);
      decrementStackUnlessInCreative(player, itemStack);
      info.setReturnValue(ActionResult.SUCCESS);
    }
  }

  @Inject(method = "tick", at = @At(value = "INVOKE", target = "net/minecraft/entity/passive/AllayEntity.isHoldingItem()Z", shift = At.Shift.AFTER))
  public void tick(CallbackInfo info) {
    if (isDancing()) {
      timeDanced += 1f;
      prevAnimFrame = nextAnimFrame;
      nextAnimFrame = getDanceProgress() ? (nextAnimFrame += 1f) : (nextAnimFrame -= 1f);
      nextAnimFrame = MathHelper.clamp(nextAnimFrame, 0f, 15f);
    } else {
      timeDanced = 0f;
      nextAnimFrame = 0f;
      prevAnimFrame = 0f;
    }
  }

  @Inject(method = "tickMovement", at = @At("TAIL"))
  public void tickMovement(CallbackInfo info) {
    if (isDancing() && shouldStopDancing() && age % 20 == 0) {
      setDancing(false);
      jukeboxPos = null;
    }
    tickDuplicationCooldown();
  }

  @Inject(method = "updateEventHandler", at = @At(value = "TAIL"))
  public void updateEventHandler(BiConsumer<EntityGameEventHandler<?>, ServerWorld> callback, CallbackInfo info) {
    if (world instanceof ServerWorld) {
      ServerWorld serverWorld = (ServerWorld) world;
      callback.accept(jukeboxEventHandler, serverWorld);
    }
  }

  @Override
  protected void initDataTracker() {
    super.initDataTracker();
    dataTracker.startTracking(DANCING, false);
    dataTracker.startTracking(CAN_DUPLICATE, true);
  }

  @Override
  public void handleStatus(byte status) {
    if (status == EntityStatuses.ADD_BREEDING_PARTICLES) {
      for (int i = 0; i < 3; ++i) {
        this.addHeartParticle();
      }
    } else {
      super.handleStatus(status);
    }
  }

  public void updateJukeboxPos(BlockPos jukeboxPos, boolean playing) {
    AllayDuplicationMod.LOGGER.info("Updating jukebox position");
    if (playing) {
      if (!isDancing()) {
        this.jukeboxPos = jukeboxPos;
        setDancing(true);
      }
    } else if (jukeboxPos.equals(this.jukeboxPos) || this.jukeboxPos == null) {
      this.jukeboxPos = null;
      setDancing(false);
    }
  }

  @Override
  public boolean isDancing() {
    return dataTracker.get(DANCING);
  }

  public void setDancing(boolean dancing) {
    if (world.isClient) {
      return;
    }
    dataTracker.set(DANCING, dancing);
  }

  public boolean shouldStopDancing() {
    return jukeboxPos == null
        || !jukeboxPos.isWithinDistance(getPos(), AllayDuplicationMod.JUKEBOX_PLAY.getRange())
        || !world.getBlockState(jukeboxPos).isOf(Blocks.JUKEBOX);
  }

  @Override
  public void startDuplicationCooldown() {
    duplicationCooldown = DUPLICATION_COOLDOWN;
    dataTracker.set(CAN_DUPLICATE, false);
  }

  public void tickDuplicationCooldown() {
    if (duplicationCooldown > 0) {
      duplicationCooldown--;
    }
    if (!world.isClient() && duplicationCooldown == 0 && !canDuplicate()) {
      dataTracker.set(CAN_DUPLICATE, true);
    }
  }

  public boolean canDuplicate() {
    return dataTracker.get(CAN_DUPLICATE);
  }

  public void decrementStackUnlessInCreative(PlayerEntity player, ItemStack stack) {
    if (!player.getAbilities().creativeMode) {
      stack.decrement(1);
    }
  }

  public boolean matchesDuplicationIngredient(ItemStack stack) {
    return DUPLICATION_INGREDIENT.test(stack);
  }

  public void duplicate() {
    AllayEntity entity = EntityType.ALLAY.create(world);
    if (entity != null) {
      entity.refreshPositionAfterTeleport(getPos());
      entity.setPersistent();
      ((AllayEntityExtended) entity).startDuplicationCooldown();
      startDuplicationCooldown();
      world.spawnEntity(entity);
    }
  }

  @Override
  public boolean getDanceProgress() {
    return (timeDanced % 55f) < 15f;
  }

  @Override
  public float getLerpedAnimationProgress(float time) {
    return MathHelper.lerp(time, prevAnimFrame, nextAnimFrame) / 15f;
  }

  public void addHeartParticle() {
    world.addParticle(
        ParticleTypes.HEART,
        getParticleX(1),
        getRandomBodyY() + 0.5,
        getParticleZ(1),
        random.nextGaussian() * 0.02,
        random.nextGaussian() * 0.02,
        random.nextGaussian() * 0.02);
  }

  class JukeboxEventListener
      implements GameEventListener {
    private final PositionSource positionSource;
    private final int range;

    public JukeboxEventListener(PositionSource positionSource, int range) {
      this.positionSource = positionSource;
      this.range = range;
    }

    @Override
    public PositionSource getPositionSource() {
      return this.positionSource;
    }

    @Override
    public int getRange() {
      return this.range;
    }

    @Override
    public boolean listen(ServerWorld world, GameEvent.Message event) {
      if (event.getEvent() == AllayDuplicationMod.JUKEBOX_PLAY) {
        AllayEntityMixin.this.updateJukeboxPos(new BlockPos(event.getEmitterPos()), true);
        return true;
      }
      if (event.getEvent() == AllayDuplicationMod.JUKEBOX_STOP_PLAY) {
        AllayEntityMixin.this.updateJukeboxPos(new BlockPos(event.getEmitterPos()), false);
        return true;
      }
      return false;
    }
  }
}
