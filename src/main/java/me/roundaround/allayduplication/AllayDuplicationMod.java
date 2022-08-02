package me.roundaround.allayduplication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.roundaround.allayduplication.mixin.GameEventInvoker;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.event.GameEvent;

public final class AllayDuplicationMod implements ModInitializer {
  public static final String MOD_ID = "allayduplication";
  public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

  public static final GameEvent JUKEBOX_PLAY = GameEventInvoker.invokeRegister("jukebox_play", 10);
  public static final GameEvent JUKEBOX_STOP_PLAY = GameEventInvoker.invokeRegister("jukebox_stop_play", 10);

  @Override
  public void onInitialize() {
  }
}
