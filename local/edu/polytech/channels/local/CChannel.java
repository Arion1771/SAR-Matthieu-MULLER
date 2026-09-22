package edu.polytech.channels.local;

import edu.polytech.channels.Channel;
import edu.polytech.utils.CircularBuffer;

public class CChannel implements Channel {

  protected CChannel(CBroker broker, int port) {
    throw new RuntimeException("NYI");
  }

  @Override
  public int read(byte[] bytes, int offset, int length) {
    throw new RuntimeException("NYI");
  }

  @Override
  public int write(byte[] bytes, int offset, int length) {
    throw new RuntimeException("NYI");
  }

  @Override
  public boolean disconnected() {
    throw new RuntimeException("NYI");
  }

  @Override
  public void disconnect() {
    throw new RuntimeException("NYI");
  }

}
