package edu.polytech.queues;

import java.util.LinkedList;

import edu.polytech.utils.Executor;

public class CMessageQueue implements MessageQueue{

	private enum State { OPEN, CLOSING, CLOSED }

	private CQueueBroker broker;
	private CMessageQueue peer;
	private Listener listener;
	private Task listenerTask;
	private LinkedList<byte[]> pending = new LinkedList<>();
	private State state = State.OPEN;
	private boolean scheduled;
	private boolean notified;

	CMessageQueue(CQueueBroker broker) {
		this.broker = broker;
		Executor.self().register(broker.getTask(), this);
	}

	CMessageQueue(CQueueBroker broker, CMessageQueue peer) {
		this(broker);
		this.peer = peer;
		peer.peer = this;
	}

	@Override
	public QueueBroker broker() {
		return broker;
	}

	@Override
	public void setListener(Listener l) {
		this.listener = l;
		this.listenerTask = currentTask();
		if (state == State.CLOSED)
			notifyClosed();
		else
			schedule();
	}

	@Override
	public boolean send(byte[] bytes, int offset, int length, SendListener l) {
		if (bytes == null || offset < 0 || length < 0 || length > bytes.length - offset)
			return false;
		if (state == State.OPEN) {
			byte[] msg = new byte[length];
			System.arraycopy(bytes, offset, msg, 0, length);
			peer.arrived(msg);
		}
		if (l != null)
			currentTask().post(() -> l.sent(bytes, offset, length));
		return true;
	}

	@Override
	public void close() {
		if (state == State.CLOSED)
			return;
		boolean wasOpen = (state == State.OPEN);
		state = State.CLOSED;
		pending.clear();
		notifyClosed();
		if (wasOpen)
			peer.closeRequested();
	}

	@Override
	public boolean closed() {
		return state == State.CLOSED;
	}

	/*
	 * A message sent by the peer, kept until delivered to the listener.
	 */
	private void arrived(byte[] msg) {
		if (state == State.CLOSED)
			return;
		pending.add(msg);
		schedule();
	}

	/*
	 * The peer has been closed: this end point becomes the closing end point.
	 * Messages already arrived are still delivered, then this end point is closed.
	 */
	private void closeRequested() {
		if (state != State.OPEN)
			return;
		state = State.CLOSING;
		schedule();
	}

	/*
	 * Delivers pending messages one at a time, in order, on the listener task.
	 */
	private void schedule() {
		if (listener == null || scheduled)
			return;
		if (pending.isEmpty()) {
			if (state == State.CLOSING) {
				state = State.CLOSED;
				notifyClosed();
			}
			return;
		}
		scheduled = true;
		listenerTask.post(() -> {
			scheduled = false;
			if (state == State.CLOSED || pending.isEmpty())
				return;
			byte[] msg = pending.removeFirst();
			try {
				listener.received(msg);
			} finally {
				schedule();
			}
		});
	}

	private void notifyClosed() {
		if (listener == null || notified)
			return;
		notified = true;
		Listener l = listener;
		listenerTask.post(() -> l.closed());
	}

	private Task currentTask() {
		Task task = Task.task();
		return (task != null) ? task : broker.getTask();
	}

}
