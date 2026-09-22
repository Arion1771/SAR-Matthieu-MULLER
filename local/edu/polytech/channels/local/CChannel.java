package edu.polytech.channels.local;

import edu.polytech.channels.Broker;
import edu.polytech.channels.Channel;
import edu.polytech.utils.CircularBuffer;

public class CChannel implements Channel {

  private static final int CAPACITY = 4096;

  protected Broker broker;
  protected int port;

  protected CircularBuffer in;
  protected CircularBuffer out;
  protected Channel OtherSide;

  private boolean localDisconnected;

  protected CChannel(Broker broker, int port) {
    this.broker = broker;
    this.port = port;
    this.in = new CircularBuffer(CAPACITY);
  }

  protected CChannel(Broker broker, int port, CChannel peer) {
    this.broker = broker;
    this.port = port;
    this.in = new CircularBuffer(CAPACITY);
    this.out = peer.in;
    this.OtherSide = peer;
    peer.out = this.in;
    peer.OtherSide = this;
  }

  @Override
  public int read(byte[] bytes, int offset, int length) {
    if (bytes == null || offset < 0 || length < 0 || offset > bytes.length) {
      throw new IllegalArgumentException();
    }
    int max = Math.min(length, bytes.length - offset);
    if (max == 0) {
      return 0;
    }
    synchronized (in) {
      while (in.empty() && !disconnected()) {
        try {
          in.wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      int n = 0;
      while (n < max && !in.empty()) {
        bytes[offset + n++] = in.pull();
      }
      in.notifyAll();
      return n;
    }
  }

  @Override
  public int write(byte[] bytes, int offset, int length) {
    if (bytes == null || offset < 0 || length < 0 || offset > bytes.length) {
      throw new IllegalArgumentException();
    }
    int max = Math.min(length, bytes.length - offset);
    if (max == 0) {
      return 0;
    }
    synchronized (out) {
      while (out.full() && !disconnected() && !(OtherSide != null && OtherSide.disconnected())) {
        try {
          out.wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      if (disconnected() || (OtherSide != null && OtherSide.disconnected())) {
        return max;
      }
      int n = 0;
      while (n < max && !out.full()) {
        out.push(bytes[offset + n++]);
      }
      out.notifyAll();
      return n;
    }
  }

  @Override
  public boolean disconnected() {
    synchronized (this) {
      if (localDisconnected) {
        return true;
      }
      return OtherSide != null && ((CChannel) OtherSide).localDisconnected && in.empty();
    }
  }

  @Override
  public void disconnect() {
    if (localDisconnected) {
      return;
    }
    localDisconnected = true;
    synchronized (in) {
      in.notifyAll();
    }
    if (out != null) {
      synchronized (out) {
        out.notifyAll();
      }
    }
  }

}
