package edu.polytech.channels.local;

import edu.polytech.channels.Bootstrap;
import edu.polytech.channels.Broker;
import edu.polytech.channels.Task;

public class Boot implements Bootstrap {

  public Boot() {
    new BrokerManager();
  }
  
  @Override
  public Broker newBroker(String name) {
    throw new RuntimeException("NYI");
  }

  @Override
  public Task newTask(Broker b, Runnable r, String name) {
    throw new RuntimeException("NYI");
  }

}
