package edu.polytech.queues;

import edu.polytech.utils.Executor;

public class Boot implements Bootstrap {

	public Boot() {
	}

	@Override
	public Task newTask(Runnable r, String name) {
		Executor executor = Executor.self();
		synchronized (executor) {
			Task task = executor.newTask(name);
			executor.post(task, r);
			return task;
		}
	}

}
