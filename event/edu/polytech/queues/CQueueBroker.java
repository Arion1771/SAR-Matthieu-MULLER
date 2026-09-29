package edu.polytech.queues;

import java.util.HashMap;
import java.util.Map;

import edu.polytech.utils.Executor;

public class CQueueBroker implements QueueBroker{

	private String name;
	private Task task;
	private Map<Integer, BindListener> binds = new HashMap<>();

	public CQueueBroker(String name) {
		if (name == null || name.isEmpty())
			throw new IllegalArgumentException("invalid broker name");
		this.name = name;
		this.task = Task.task();
		if (task == null)
			throw new IllegalStateException("a broker must be created by a task");
		QueueBrokerManager.instance.add(this);
		if (task.getBroker() == null)
			Executor.self().set(task, this);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Task getTask() {
		return task;
	}

	@Override
	public boolean bind(int port, BindListener listener) {
		if (!validPort(port) || listener == null || binds.containsKey(port))
			return false;
		binds.put(port, listener);
		return true;
	}

	@Override
	public boolean unbind(int port) {
		BindListener listener = binds.remove(port);
		if (listener == null)
			return false;
		task.post(() -> listener.unbound());
		return true;
	}

	@Override
	public boolean connect(String name, int port, ConnectListener listener) {
		if (name == null || name.isEmpty() || !validPort(port) || listener == null)
			return false;
		CQueueBroker remote = QueueBrokerManager.instance.get(name);
		if (remote == null)
			return false;
		remote.task.post(() -> remote.accept(this, port, listener));
		return true;
	}

	/*
	 * Executed on the task of this broker, the remote side of the connect.
	 */
	private void accept(CQueueBroker from, int port, ConnectListener listener) {
		BindListener bindListener = binds.get(port);
		if (bindListener == null) {
			from.task.post(() -> listener.refused());
			return;
		}
		CMessageQueue local = new CMessageQueue(this);
		CMessageQueue remote = new CMessageQueue(from, local);
		task.post(() -> bindListener.accepted(local));
		from.task.post(() -> listener.connected(remote));
	}

	private static boolean validPort(int port) {
		return port >= 0 && port <= 65535;
	}

}
