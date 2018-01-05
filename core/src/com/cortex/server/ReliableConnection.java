package com.cortex.server;

import com.badlogic.gdx.utils.Array;
import com.cortex.server.packets.ReliableUDPPacket;

/**
 * Created by tanner on 5/15/17.
 */
public class ReliableConnection {

  public static final int BUFFER_SIZE = 32;

  public ReliableUDPPacket[] sentBuffer, recvBuffer, msgSequence;
  public ReliableUDPPacket lastRecv, lastSent;

  public int recvMsgId = 0;

  public ReliableConnection() {
    sentBuffer = new ReliableUDPPacket[BUFFER_SIZE];
    recvBuffer = new ReliableUDPPacket[BUFFER_SIZE];
    msgSequence = new ReliableUDPPacket[BUFFER_SIZE];
  }

  public void insertPacketSent(ReliableUDPPacket packet) {
    int index = packet.sequence % BUFFER_SIZE;
    sentBuffer[index] = packet;
  }

  public ReliableUDPPacket getPacketSent(int seq) {
    if (lastSent.sequence == seq) return lastSent;
    int index = seq % BUFFER_SIZE;
    if (sentBuffer[index].sequence == seq)
      return sentBuffer[index];
    else
      return null;
  }

  public void insertPacketRecv(ReliableUDPPacket packet) {
    int index = packet.sequence % BUFFER_SIZE;
    recvBuffer[index] = packet;
  }

  public void insertMsg(ReliableUDPPacket p) {
    int index = p.sequence % BUFFER_SIZE;
    msgSequence[index] = p;
  }

  public ReliableUDPPacket getPacketRecv(int seq) {
    if (lastRecv.sequence == seq) return lastRecv;
    int index = seq % BUFFER_SIZE;
    if (recvBuffer[index].sequence == seq)
      return recvBuffer[index];
    else
      return null;
  }

  public void onPacketAcked(int seq) {
    if (lastSent != null && lastSent.sequence == seq) {
      lastSent.acked = true;
      return;
    }
    int index = seq % BUFFER_SIZE;
    if (sentBuffer[index] != null && sentBuffer[index].sequence == seq)
      sentBuffer[index].acked = true;
  }

  public void insertLostPackets(ReliableUDPPacket p) {
    if (p.lostPackets != null) {
      for (ReliableUDPPacket rp : p.lostPackets) {
        insertPacketRecv(rp);
        insertMsg(rp);
      }
    }
  }

  public ReliableUDPPacket sendReliableUDP(Object obj) {

    // 1. Insert an entry for the current send packet sequence number in the sent packet
    //    sequence buffer with data indicating that it hasn't been acked yet.
    ReliableUDPPacket rp = new ReliableUDPPacket();
    rp.sequence = lastSent != null ? lastSent.sequence + 1 : 0;
    rp.acked = false;

    // 2. Generate ack and ackBits from the contents of the local received packet sequence buffer
    //    and the most recent received packet sequence number.
    int ack = lastRecv != null ? lastRecv.sequence : 0;
    int ackBits = 0;
    if (lastRecv != null) {
      for (int i = 0; i < BUFFER_SIZE; i++) {
        if (recvBuffer[i] != null) {
          int mask = 0x1 << (lastRecv.sequence - recvBuffer[i].sequence - 1);
          ackBits = ackBits | mask;
        }
      }
    }
    // 3. Fill packet header with sequence, ack, ackBits
    rp.ack = ack;
    rp.ackBits = ackBits;

    // Attach payload
    rp.object = obj;
    rp.timeSent = System.currentTimeMillis();

    // Insert in sent buffer
    if (lastSent != null)
      insertPacketSent(lastSent);
    lastSent = rp;

    // Check for "lost" packets
    Array<ReliableUDPPacket> lostPackets = new Array<ReliableUDPPacket>();
    for (int i = 0; i < BUFFER_SIZE; i++) {
      if (sentBuffer[i] != null)
        // If it has been 100ms and we haven't recieved an ack, resend that packet
        if (!sentBuffer[i].acked && (System.currentTimeMillis() - sentBuffer[i].timeSent > 100)) {
          sentBuffer[i].lostPackets = null;
          lostPackets.add(sentBuffer[i]);
        }
    }
    rp.lostPackets = lostPackets.toArray(ReliableUDPPacket.class);

    // 4. Send the packet
    return rp;
  }


}
