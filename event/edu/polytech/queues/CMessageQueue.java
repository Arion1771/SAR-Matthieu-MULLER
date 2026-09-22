package edu.polytech.queues;

public class CMessageQueue implements MessageQueue{

	@Override
	public QueueBroker broker() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setListener(Listener l) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean send(byte[] bytes, int offset, int length, SendListener l) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean closed() {
		// TODO Auto-generated method stub
		return false;
	}

}
