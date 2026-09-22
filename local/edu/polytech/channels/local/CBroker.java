package edu.polytech.channels.local;

import java.util.HashMap;
import java.util.Map;

import edu.polytech.channels.Broker;
import edu.polytech.channels.Channel;

public class CBroker implements Broker {

  protected String name;
  protected Map<Integer, RendezVous> rendezvous = new HashMap<>();

  CBroker(String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("invalid broker name");
    }
    this.name = name;
    synchronized (BrokerManager.instance) {
      if (BrokerManager.instance.get(name) != null) {
        throw new IllegalArgumentException("broker name already used");
      }
      BrokerManager.instance.add(this);
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Channel connect(String name, int port) {
    if (name == null || name.isEmpty() || port < 0) {
      throw new IllegalArgumentException();
    }
    CBroker target = BrokerManager.instance.get(name);
    if (target == null) {
      return null;
    }
    RendezVous rdv;
    synchronized (target.rendezvous) {
      rdv = target.rendezvous.get(port);
      if (rdv == null) {
        rdv = new RendezVous(target, port);
        target.rendezvous.put(port, rdv);
      }
    }
    return rdv.connect(this);
  }

  @Override
  public Channel accept(int port) {
    if (port < 0) {
      throw new IllegalArgumentException();
    }
    RendezVous rdv;
    synchronized (rendezvous) {
      rdv = rendezvous.get(port);
      if (rdv == null) {
        rdv = new RendezVous(this, port);
        rendezvous.put(port, rdv);
      }
    }
    return rdv.accept();
  }

}
