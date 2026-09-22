/*
 * Copyright (C) 2023 Pr. Olivier Gruber                                    
 *                                                                       
 * This program is free software: you can redistribute it and/or modify  
 * it under the terms of the GNU General Public License as published by  
 * the Free Software Foundation, either version 3 of the License, or     
 * (at your option) any later version.                                   
 *                                                                       
 * This program is distributed in the hope that it will be useful,       
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         
 * GNU General Public License for more details.                          
 *                                                                       
 * You should have received a copy of the GNU General Public License     
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package edu.polytech.utils;

import edu.polytech.queues.CMessageQueue;
import edu.polytech.queues.CQueueBroker;
import edu.polytech.queues.Task;

public class Executor extends Thread {

  private class Event {
    CTask task;
    Runnable r;
    long eta;
    Event next;

    Event(CTask t, Runnable r) {
      this.task = t;
      this.r = r;
    }

    Event(Event last, CTask t, Runnable r) {
      this.task = t;
      this.r = r;
      last.next = this;
    }

    Event(CTask t, Runnable r, long eta) {
      this.task = t;
      this.r = r;
      this.eta = eta;
    }

    Event(Event prev, CTask t, Runnable r, long eta) {
      this.task = t;
      this.r = r;
      this.eta = eta;
      this.next = prev.next;
      prev.next = this;
    }

    Event(CTask t, Runnable r, long eta, Event next) {
      this.task = t;
      this.r = r;
      this.eta = eta;
      this.next = next;
    }
  }

  static Executor self;
  static {
    self = new Executor();
    self.start();
  }

  public static Executor self() {
    return self;
  }

  static boolean VERBOSE = true;

  static void log(String s) {
    if (VERBOSE)
      System.out.println(s);
  }

  static void log(String s, Throwable cause) {
    if (VERBOSE) {
      System.err.println(s);
      cause.printStackTrace(System.err);
    }
  }

  Task task;
  Event queue, last;
  Event delayed;

  private Executor() {
    super("Event Pump");
    self = this;
  }

  public static Task task() {
    return self.task;
  }

  public void run() {
    while (true) {
      Event e = null;
      while (e == null) {
        long now = System.currentTimeMillis();
        checkDelayed(now);
        synchronized (this) {
          if (queue == null)
            sleep();
          e = queue;
          if (queue == last)
            queue = last = null;
          else
            queue = queue.next;
        }
      }
      task = e.task;
      if (!task.dead()) {
        try {
          e.r.run();
        } catch (Throwable th) {
          task.fail(th);
        }
      }
      task = null;
    }
  }

  private void checkDelayed(long now) {
    Event e = delayed;
    while (e != null && e.eta <= now) {
      post(e.task, e.r);
      delayed = delayed.next;
      e = delayed;
    }
  }

  public void set(Task t, CQueueBroker qb) {
    CTask task = (CTask) t;
    task.set(qb);
  }
  
  public void register(Task t, CMessageQueue mq) {
    CTask task = (CTask) t;
    task.register(mq);
  }

  public Task newTask(String name) {
    CTask task = new CTask(name);
    return task;
  }

  public void post(Task t, Runnable r) {
    CTask task = (CTask) t;
    if (last == null)
      queue = last = new Event(task, r);
    else
      last = new Event(last, task, r);
  }

  public void post(Task t, Runnable r, int delay) {
    CTask task = (CTask) t;
    long eta = System.currentTimeMillis() + delay;
    if (delayed == null)
      delayed = new Event(task, r, eta);
    else {
      Event p = null, e = delayed;
      while (e != null && e.eta <= eta) {
        p = e;
        e = e.next;
      }
      if (p == null) {
        delayed = new Event(task, r, eta, delayed);
      } else {
        new Event(p, task, r, eta);
      }
    }
  }

  private void sleep() {
    while (queue != null) {
      try {
        wait(100);
        checkDelayed(System.currentTimeMillis());
      } catch (InterruptedException ex) {
        // nothing to do here.
      }
    }
  }

  public static void check() {
    Thread thread = Thread.currentThread();
    if (!(thread instanceof Executor)) {
      failStop("Executor: wrong thread");
    }
  }

  public static void ensure(boolean cond) {
    if (!cond)
      failStop();
  }

  public static void failStop() {
    failStop("fail stop.", null);
  }

  public static void failStop(String msg) {
    failStop(msg, null);
  }

  public static void failStop(String msg, Throwable th) {
    if (th == null) {
      try {
        throw new Error();
      } catch (Error e) {
        th = e;
      }
    }
    synchronized (System.out) {
      synchronized (System.err) {
        System.err.println("\n\n==============================");
        System.err.println("PANIC: " + msg);
        th.printStackTrace(System.err);
        System.err.flush();
        System.exit(-1);
      }
    }
  }

  public static void failStop(Throwable th) {
    failStop("fail stop.", th);
  }

}
