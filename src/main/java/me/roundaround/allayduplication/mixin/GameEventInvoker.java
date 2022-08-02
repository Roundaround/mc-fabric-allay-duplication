package me.roundaround.allayduplication.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.event.GameEvent;

@Mixin(GameEvent.class)
public interface GameEventInvoker {
  @Invoker("register")
  public static GameEvent invokeRegister(String id, int range) {
    throw new AssertionError();
  }
}
