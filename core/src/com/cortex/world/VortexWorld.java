package com.cortex.world;

import com.cortex.Vortex;
import com.cortex.server.packets.*;

/**
 * Created by tanner on 5/15/17.
 */
public class VortexWorld {

  public static final int TICK_FREQUENCY = 20;
  private boolean running;

  private long dt, currentTime, accumulator;
  private int tick;

  public static WorldState state = WorldState.LOADING;

  public VortexWorld() {
    running = false;
  }

  public void run() {
    running = true;
    tick = 0;
    dt = 1000000000 / TICK_FREQUENCY;

    currentTime = System.nanoTime();
    accumulator = 0;

    state = WorldState.LOADING;
    // Load world, get shit ready

    while (running) {
      long newTime = System.nanoTime();
      long frameTime = newTime - currentTime;
      if (frameTime > 250000000) //  limit to 250ms
        frameTime = 250000000;
      currentTime = newTime;

      accumulator += frameTime;

      while (accumulator >= dt) {
        // prevState = currState;
        // calc next state
        tick++;
        accumulator -= dt;
      }
      try {
        Thread.yield();
        Thread.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      if (state != WorldState.LIVE) running = false;
    }

    state = WorldState.SHUTTING_DOWN;
    // Save everything, get ready for next start up.
  }

}
