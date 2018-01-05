package com.cortex.server;

import com.cortex.server.packets.*;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.util.HashMap;

/**
 * Created by tanner on 5/15/17.
 */
public class VortexServer extends Server {

  HashMap<Integer, ReliableConnection> reliableConnections;

  public VortexServer() {
    super();

    reliableConnections = new HashMap<>();

    getKryo().register(ReliableUDPPacket.class);
    getKryo().register(ReliableUDPPacket[].class);
    getKryo().register(WorldSnapshot.class);
    getKryo().register(LoginRequest.class);
    getKryo().register(LoginResponse.class);

    addListener(new Listener() {
      @Override
      public void connected(Connection con) {
        reliableConnections.put(con.getID(), new ReliableConnection());
      }

      @Override
      public void disconnected(Connection con) {
        reliableConnections.remove(con.getID());
      }

      @Override
      public void received(Connection con, Object obj) {

        if (obj instanceof ReliableUDPPacket) {
          if (reliableConnections.containsKey(con.getID())) { // if we actually have that connection
            ReliableConnection c = reliableConnections.get(con.getID());
            ReliableUDPPacket p = (ReliableUDPPacket) obj;

            // Ack algorithm from gafferongames.com
            // 1. Read in sequence from packet header
            // 2. If sequence is more recent than the previous most recent received packet sequence number,
            //    update the most recent received packet sequence number
            // 3. Insert an entry for this packet in the recieved packet sequence buffer.
            if (c.lastRecv == null) {
              c.lastRecv = p;
            } else if (p.sequence > c.lastRecv.sequence) {
              c.insertPacketRecv(c.lastRecv);
              c.lastRecv = p;
            } else {
              c.insertPacketRecv(p);
            }
            c.insertMsg(p);
            c.insertLostPackets(p);

            // 4. Decode the set of acked packet sequence numbers from ack and ackBits
            // 5. Iterate across all acked packet sequence numbers and mark those packets acked
            c.onPacketAcked(p.ack);
            if (c.lastSent != null) {
              for (int i = 0; i < c.BUFFER_SIZE; i++) {
                if (c.sentBuffer[i] != null) {
                  int mask = 0x1 << (c.lastSent.sequence - c.sentBuffer[i].sequence - 1);
                  if ((p.ackBits & mask) != 0) {
                    c.sentBuffer[i].acked = true;
                  }
                }
              }
            }
            // End ack algorithm

            // Ordered messsage processing
            int nextMsgIndex = c.recvMsgId % c.BUFFER_SIZE;
            // If the next message id we have stored is greater than the next expected message id, then we have
            // lost that expected message id and need to try the next best message
            while (c.msgSequence[nextMsgIndex] != null && c.msgSequence[nextMsgIndex].sequence > c.recvMsgId) {
              c.recvMsgId++;
              nextMsgIndex = c.recvMsgId % c.BUFFER_SIZE;
            }
            // Process all available packets in order
            while (c.msgSequence[nextMsgIndex] != null && c.msgSequence[nextMsgIndex].sequence == c.recvMsgId) {
              processPacket(con, c.msgSequence[nextMsgIndex].object);
              c.recvMsgId++;
              nextMsgIndex = c.recvMsgId % c.BUFFER_SIZE;
            }
          }
        } else {
          processPacket(con, obj);
        }
      }

      @Override
      public void idle(Connection con) {
        // reduce PPS?
      }
    });
  }

  public synchronized void processPacket(Connection con, Object obj) {
    if (obj instanceof LoginRequest) {
      LoginRequest lreq = (LoginRequest) obj;
      LoginResponse lr = new LoginResponse();
      lr.authenticated = true;
      sendToTCP(con.getID(), lr);
    }
  }

  public void sendToReliableUDP(int connectionId, Object obj) {
    if (reliableConnections.containsKey(connectionId)) {
      ReliableConnection rc = reliableConnections.get(connectionId);
      ReliableUDPPacket p = rc.sendReliableUDP(obj);
      sendToUDP(connectionId, p);
      reliableConnections.put(connectionId, rc);
    }
  }

  public void sendToAllReliableUDP (Object object) {
    Connection[] connections = getConnections();
    for (int i = 0, n = connections.length; i < n; i++) {
      Connection connection = connections[i];
      sendToReliableUDP(connection.getID(), object);
    }
  }

}
