package io.github.kinsleykajiva.ice;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages the GLib Main Context and Main Loop.
 * libnice requires a running GLib event loop for candidate gathering and signaling.
 */
public class GLibContext implements AutoCloseable {
    private final MemorySegment loop;
    private final MemorySegment context;
    private final Arena arena;
    private final ExecutorService executor;

    public GLibContext() {
        this.arena = Arena.ofShared();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "GLib-MainLoop");
            t.setDaemon(true);
            return t;
        });

        try {
            if (NiceBindings.g_main_context_new != null) {
                this.context = (MemorySegment) NiceBindings.g_main_context_new.invokeExact();
                if (NiceBindings.g_main_loop_new != null) {
                    this.loop = (MemorySegment) NiceBindings.g_main_loop_new.invokeExact(context, 0);
                } else {
                    this.loop = MemorySegment.NULL;
                }
            } else {
                this.context = MemorySegment.NULL;
                this.loop = MemorySegment.NULL;
            }
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create GLibContext", t);
        }
    }

    /**
     * Starts the GLib main loop in a background thread.
     */
    public void start() {
        executor.submit(() -> {
            try {
                if (NiceBindings.g_main_loop_run != null && !loop.equals(MemorySegment.NULL)) {
                    NiceBindings.g_main_loop_run.invokeExact(loop);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }

    /**
     * Stops the GLib main loop.
     */
    public void stop() {
        if (!loop.equals(MemorySegment.NULL)) {
            try {
                if (NiceBindings.g_main_loop_quit != null) {
                    NiceBindings.g_main_loop_quit.invokeExact(loop);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    @Override
    public void close() {
        stop();
        executor.shutdownNow();
        if (!loop.equals(MemorySegment.NULL)) {
            try {
                if (NiceBindings.g_main_loop_unref != null) {
                    NiceBindings.g_main_loop_unref.invokeExact(loop);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        if (!context.equals(MemorySegment.NULL)) {
            try {
                if (NiceBindings.g_main_context_unref != null) {
                    NiceBindings.g_main_context_unref.invokeExact(context);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        arena.close();
    }

    public MemorySegment getContext() {
        return context;
    }
}
