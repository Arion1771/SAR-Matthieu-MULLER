package edu.polytech.channels.local;

import java.util.LinkedList;
import java.util.Queue;

import edu.polytech.channels.Broker;
import edu.polytech.channels.Channel;

public class RendezVous {

  private static class Pending {
    Broker from;
    Channel channel;

    Pending(Broker from) {
      this.from = from;
    }
  }

  protected Broker broker;
  protected int port;
  protected Queue<Pending> waiting = new LinkedList<>();
  protected boolean accepting;

  public RendezVous(Broker broker, int port) {
    this.broker = broker;
    this.port = port;
  }

  public synchronized Channel connect(Broker from) {
    Pending request = new Pending(from);
    waiting.add(request);
    notifyAll();
    while (request.channel == null) {
      try {
        wait();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    return request.channel;
  }

  public synchronized Channel accept() {
    while (accepting) {
      try {
        wait();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    accepting = true;
    try {
      while (waiting.isEmpty()) {
        try {
          wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      Pending request = waiting.poll();
      CChannel server = new CChannel(broker, port);
      CChannel client = new CChannel(request.from, port, server);
      request.channel = client;
      notifyAll();
      return server;
    } finally {
      accepting = false;
      notifyAll();
    }
  }

}
