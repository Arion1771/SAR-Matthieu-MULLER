package edu.polytech.queues;

import java.util.HashMap;
import java.util.Map;

public class QueueBrokerManager {

	static final QueueBrokerManager instance = new QueueBrokerManager();

	private Map<String, CQueueBroker> brokers = new HashMap<>();

	private QueueBrokerManager() {
	}

	public synchronized void add(CQueueBroker broker) {
		if (brokers.containsKey(broker.getName()))
			throw new IllegalArgumentException("broker name already used");
		brokers.put(broker.getName(), broker);
	}

	public synchronized void remove(CQueueBroker broker) {
		brokers.remove(broker.getName());
	}

	public synchronized CQueueBroker get(String name) {
		return brokers.get(name);
	}

}
