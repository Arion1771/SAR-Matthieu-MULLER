package edu.polytech.channels.local;

import java.util.HashMap;
import java.util.Map;

public class BrokerManager {

  static BrokerManager instance;

  protected Map<String, CBroker> brokers = new HashMap<>();

  BrokerManager() {
    instance = this;
  }

  public synchronized void add(CBroker broker) {
    brokers.put(broker.getName(), broker);
  }

  public synchronized void remove(CBroker broker) {
    brokers.remove(broker.getName());
  }

  public synchronized CBroker get(String name) {
    return brokers.get(name);
  }

}
