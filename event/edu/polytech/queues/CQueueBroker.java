package edu.polytech.queues;

public class CQueueBroker implements QueueBroker{

	public CQueueBroker(String name) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Task getTask() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean bind(int port, BindListener listener) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean unbind(int port) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean connect(String name, int port, ConnectListener listener) {
		// TODO Auto-generated method stub
		return false;
	}

}
