package edu.polytech.channels.local;

import java.util.HashMap;

import edu.polytech.channels.Broker;
import edu.polytech.channels.Channel;

public class CBroker implements Broker {

  CBroker(String name) {
    throw new RuntimeException("NYI");
  }

  @Override
  public String getName() {
    throw new RuntimeException("NYI");
  }

  @Override
  public Channel connect(String name, int port) {
    throw new RuntimeException("NYI");
  }

  @Override
  public Channel accept(int port) {
    throw new RuntimeException("NYI");
  }

}
