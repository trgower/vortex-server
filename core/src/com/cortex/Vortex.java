package com.cortex;

import com.cortex.server.VortexServer;
import com.cortex.world.VortexWorld;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by tanner on 5/15/17.
 */
public class Vortex {

  public static ConcurrentHashMap<Integer, String> players = new ConcurrentHashMap<>();
  public static VortexServer server = new VortexServer();
  public static VortexWorld world = new VortexWorld();

  public static void main(String args[]) {
    server.start();
    try {
      server.bind(5555, 5556);
    } catch (IOException e) {
      e.printStackTrace();
    }

    // load shit
    // do other shit
    // start world loop
    // world.run();
  }

}
