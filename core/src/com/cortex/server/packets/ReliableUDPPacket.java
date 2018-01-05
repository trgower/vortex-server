package com.cortex.server.packets;

/**
 * Created by tanner on 5/11/17.
 */
public class ReliableUDPPacket {

  // Header
  public int sequence;
  public int ack;
  public int ackBits;
  public boolean acked;
  public long timeSent;

  // Payload
  public Object object;
  public ReliableUDPPacket[] lostPackets;

}
