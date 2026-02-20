package demo;

import io.github.kinsleykajiva.ice.*;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;

/**
 * Multi-threaded ICE Demo.
 * Demonstrates running two ICE agents in separate threads and establishing a connection between them.
 */
public class ThreadedIceDemo {
    private static final Exchanger<String> sdpExchanger = new Exchanger<>();
    private static final Map<Long, AgentContext> contexts = new ConcurrentHashMap<>();
    private static final CountDownLatch exitLatch = new CountDownLatch(2);

    static class AgentContext {
        String name;
        NiceAgent agent;
        int streamId;
        CountDownLatch gatheringDone = new CountDownLatch(1);
        CountDownLatch ready = new CountDownLatch(1);

        AgentContext(String name, NiceAgent agent) {
            this.name = name;
            this.agent = agent;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("--- Multi-threaded ICE Demo Starting ---");

        Thread threadA = new Thread(() -> runAgent("AgentA"), "Thread-AgentA");
        Thread threadB = new Thread(() -> runAgent("AgentB"), "Thread-AgentB");

        threadA.start();
        threadB.start();

        if (!exitLatch.await(60, TimeUnit.SECONDS)) {
            System.err.println("Demo timed out after 60 seconds!");
        }
        System.out.println("--- Multi-threaded ICE Demo Finished ---");
        System.exit(0);
    }

    private static void runAgent(String name) {
        try (GLibContext glib = new GLibContext();
             NiceAgent agent = new NiceAgent(glib.getContext(), NiceBindings.NICE_COMPATIBILITY_RFC5245)) {

            System.out.println("[" + name + "] Thread started. Initializing agent...");
            glib.start();
            glib.pushThreadDefault();

            AgentContext ctx = new AgentContext(name, agent);
            // We use the address of a shared memory segment as a unique ID for callbacks
            Arena callbackArena = Arena.ofShared();
            MemorySegment idPtr = callbackArena.allocate(ValueLayout.JAVA_LONG);
            long id = idPtr.address();
            contexts.put(id, ctx);

            // Configure agent
            agent.setControllingMode(name.equals("AgentA"));
            agent.disableExtraFeatures();
            agent.setStunServer("stun.l.google.com", 19302);

            // 1. Setup signal callbacks (gathering-done, state-changed, new-candidate)
            setupCallbacks(agent, callbackArena, idPtr);

            // 2. Add stream
            ctx.streamId = agent.addStream(1);
            if (ctx.streamId <= 0) {
                System.err.println("[" + name + "] Failed to add stream!");
                return;
            }

            // 3. Attach receiver (MUST be called before gathering candidates in some versions/platforms)
            setupReceiver(agent, callbackArena, idPtr, ctx.streamId, glib.getContext());

            // 4. Start gathering candidates
            boolean success = agent.gatherCandidates(ctx.streamId);
            System.out.println("[" + name + "] Added stream " + ctx.streamId + ". Gathering candidates... Success: " + success);

            // 5. Wait for candidates to be gathered
            if (!ctx.gatheringDone.await(30, TimeUnit.SECONDS)) {
                 System.err.println("[" + name + "] Candidate gathering timed out after 30s!");
            }

            // 6. Generate local SDP and exchange with peer
            String localSdp = agent.generateLocalSdp();
            System.out.println("[" + name + "] Local SDP generated:\n" + localSdp);
            System.out.println("[" + name + "] Waiting for peer exchange...");

            String remoteSdp = sdpExchanger.exchange(localSdp);
            System.out.println("[" + name + "] Received remote SDP. Parsing...");

            // 5. Parse remote SDP to start connectivity checks
            agent.parseRemoteSdp(remoteSdp);

            // 6. Wait for ICE state to reach READY
            if (ctx.ready.await(30, TimeUnit.SECONDS)) {
                System.out.println("[" + name + "] ICE CONNECTED & READY!");
                
            // 8. Send test messages
                Thread.sleep(1500); 
                String msg = "Hello from " + name + " (Thread ID: " + Thread.currentThread().getId() + ")";
                System.out.println("[" + name + "] Sending: " + msg);
                agent.send(ctx.streamId, 1, msg.getBytes());
                
                // Keep thread alive to receive peer's message
                Thread.sleep(5000); 
            } else {
                System.err.println("[" + name + "] Failed to establish ICE connection (timeout).");
            }

            glib.popThreadDefault();
            System.out.println("[" + name + "] Agent shutting down.");
            exitLatch.countDown();
            callbackArena.close();
        } catch (Exception e) {
            System.err.println("[" + name + "] Fatal Error: " + e.getMessage());
            e.printStackTrace();
            exitLatch.countDown();
        }
    }

    private static void setupCallbacks(NiceAgent agent, Arena arena, MemorySegment data) throws NoSuchMethodException, IllegalAccessException {
        // Signal: candidate-gathering-done (void (*)(NiceAgent*, guint, gpointer))
        MemorySegment gatherCb = Linker.nativeLinker().upcallStub(
            MethodHandles.lookup().findStatic(ThreadedIceDemo.class, "onGatheringDone",
                MethodType.methodType(void.class, MemorySegment.class, int.class, MemorySegment.class)),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS),
            arena
        );
        agent.connectSignal("candidate-gathering-done", gatherCb, data);

        // Signal: component-state-changed (void (*)(NiceAgent*, guint, guint, guint, gpointer))
        MemorySegment stateCb = Linker.nativeLinker().upcallStub(
            MethodHandles.lookup().findStatic(ThreadedIceDemo.class, "onStateChanged",
                MethodType.methodType(void.class, MemorySegment.class, int.class, int.class, int.class, MemorySegment.class)),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS),
            arena
        );
        agent.connectSignal("component-state-changed", stateCb, data);

        // Signal: new-candidate (void (*)(NiceAgent*, guint, guint, gchar*, gpointer))
        MemorySegment candCb = Linker.nativeLinker().upcallStub(
            MethodHandles.lookup().findStatic(ThreadedIceDemo.class, "onNewCandidate",
                MethodType.methodType(void.class, MemorySegment.class, int.class, int.class, MemorySegment.class, MemorySegment.class)),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
            arena
        );
        agent.connectSignal("new-candidate", candCb, data);
    }

    private static void setupReceiver(NiceAgent agent, Arena arena, MemorySegment data, int streamId, MemorySegment context) throws NoSuchMethodException, IllegalAccessException {
        // Callback: NiceAgentRecvFunc (void (*)(NiceAgent*, guint, guint, guint, gchar*, gpointer))
        MemorySegment recvCb = Linker.nativeLinker().upcallStub(
            MethodHandles.lookup().findStatic(ThreadedIceDemo.class, "onDataReceived",
                MethodType.methodType(void.class, MemorySegment.class, int.class, int.class, int.class, MemorySegment.class, MemorySegment.class)),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
            arena
        );
        agent.attachReceiver(streamId, 1, context, recvCb, data);
    }

    // --- Native Callbacks (called from GLib main loop threads) ---

    public static void onGatheringDone(MemorySegment agentPtr, int streamId, MemorySegment dataPtr) {
        AgentContext ctx = contexts.get(dataPtr.address());
        if (ctx != null) {
            System.out.println("[" + ctx.name + "] Signal: candidate-gathering-done for stream " + streamId);
            ctx.gatheringDone.countDown();
        }
    }

    public static void onStateChanged(MemorySegment agentPtr, int streamId, int componentId, int state, MemorySegment dataPtr) {
        AgentContext ctx = contexts.get(dataPtr.address());
        if (ctx != null) {
            String stateName = switch(state) {
                case NiceBindings.NICE_COMPONENT_STATE_DISCONNECTED -> "DISCONNECTED";
                case NiceBindings.NICE_COMPONENT_STATE_GATHERING -> "GATHERING";
                case NiceBindings.NICE_COMPONENT_STATE_CONNECTING -> "CONNECTING";
                case NiceBindings.NICE_COMPONENT_STATE_CONNECTED -> "CONNECTED";
                case NiceBindings.NICE_COMPONENT_STATE_READY -> "READY";
                case NiceBindings.NICE_COMPONENT_STATE_FAILED -> "FAILED";
                default -> "UNKNOWN (" + state + ")";
            };
            System.out.println("[" + ctx.name + "] State Changed -> " + stateName);
            if (state == NiceBindings.NICE_COMPONENT_STATE_READY) {
                ctx.ready.countDown();
            }
        }
    }

    public static void onDataReceived(MemorySegment agentPtr, int streamId, int componentId, int len, MemorySegment buf, MemorySegment dataPtr) {
        AgentContext ctx = contexts.get(dataPtr.address());
        if (ctx != null) {
            byte[] bytes = buf.reinterpret(len).toArray(ValueLayout.JAVA_BYTE);
            System.out.println("[" + ctx.name + "] << RECEIVED: " + new String(bytes));
        }
    }

    public static void onNewCandidate(MemorySegment agentPtr, int streamId, int componentId, MemorySegment candPtr, MemorySegment dataPtr) {
        AgentContext ctx = contexts.get(dataPtr.address());
        if (ctx != null) {
            String cand = candPtr.reinterpret(Long.MAX_VALUE).getString(0);
            System.out.println("[" + ctx.name + "] Signal: new-candidate -> " + cand);
        }
    }
}
