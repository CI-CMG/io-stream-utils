package edu.colorado.cires.cmg.iostream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class Pipe {

  private static final AtomicLong threadCounter = new AtomicLong();
  private static final ExecutorService pool = Executors.newCachedThreadPool(r -> new Thread(r, "Pipe-" + threadCounter.incrementAndGet()));

  public static void pipe(Consumer<OutputStream> pipeSupplier, Consumer<InputStream> pipeConsumer) {

    final List<Throwable> errors = Collections.synchronizedList(new ArrayList<>(0));

    final PipedOutputStream pout = new PipedOutputStream();
    final PipedInputStream pin = new PipedInputStream();
    try {
      pin.connect(pout);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create pipe", e);
    }

    Future<?> pipeWriter = pool.submit(() -> {
      try {
        pipeSupplier.accept(pout);
      } catch (Throwable t) {
        errors.add(t);
      } finally {
        try {
          pout.close();
        } catch (Throwable t) {
          errors.add(t);
        }
      }
    });

    Future<?> pipeReader = pool.submit(() -> {
      try {
        pipeConsumer.accept(pin);
      } catch (Throwable t) {
        errors.add(t);
      } finally {
        try {
          pin.close();
        } catch (Throwable t) {
          errors.add(t);
        }
      }
    });

    InterruptedException ie = null;

    try {
      pipeWriter.get();
    } catch (InterruptedException e) {
      ie = e;
    } catch (ExecutionException e) {
      errors.add(e);
    }
    try {
      pipeReader.get();
    } catch (InterruptedException e) {
      ie = e;
    } catch (ExecutionException e) {
      errors.add(e);
    }

    if (ie != null) {
      Thread.currentThread().interrupt();
      errors.add(ie);
    }

    if (!errors.isEmpty()) {
      RuntimeException main = new RuntimeException("An error occurred in the pipe", errors.get(0));
      for (Throwable t : errors.subList(1, errors.size())) {
        main.addSuppressed(t);
      }
      throw main;
    }

  }

}
