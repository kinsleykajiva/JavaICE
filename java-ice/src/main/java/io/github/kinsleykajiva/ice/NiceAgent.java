package io.github.kinsleykajiva.ice;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * High-level wrapper for NiceAgent.
 * Handles ICE agent lifecycle and stream management.
 */
public class NiceAgent implements AutoCloseable {
    private final MemorySegment agentHandle;
    private final Arena arena;

    /**
     * Creates a new NiceAgent.
     * 
     * @param mainContext The GLib main context to use (can be null for default).
     * @param compatibility The NICE compatibility mode.
     */
    public NiceAgent(MemorySegment mainContext, int compatibility) {
        this.arena = Arena.ofShared();
        try {
            if (NiceBindings.nice_agent_new != null) {
                MemorySegment ctx = (mainContext == null) ? MemorySegment.NULL : mainContext;
                this.agentHandle = (MemorySegment) NiceBindings.nice_agent_new.invokeExact(ctx, compatibility);
            } else {
                this.agentHandle = MemorySegment.NULL;
            }
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create NiceAgent", t);
        }
    }


    /**
     * Adds a new stream to the agent.
     * 
     * @param nComponents Number of components in the stream.
     * @return The stream ID.
     */
    public int addStream(int nComponents) {
        try {
            if (NiceBindings.nice_agent_add_stream != null) {
                return (int) NiceBindings.nice_agent_add_stream.invokeExact(agentHandle, nComponents);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return 0;
    }

    /**
     * Starts gathering candidates for the given stream.
     * 
     * @param streamId The stream ID.
     * @return true if gathering started successfully.
     */
    public boolean gatherCandidates(int streamId) {
        try {
            if (NiceBindings.nice_agent_gather_candidates != null) {
                int result = (int) NiceBindings.nice_agent_gather_candidates.invokeExact(agentHandle, streamId);
                return result != 0;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return false;
    }

    /**
     * Sets the STUN server address.
     * 
     * @param server The STUN server address string.
     * @param port The STUN server port.
     */
    public void setStunServer(String server, int port) {
        try (var localArena = Arena.ofConfined()) {
            MemorySegment cStunServer = localArena.allocateFrom("stun-server");
            MemorySegment cServer = localArena.allocateFrom(server);
            
            // string property
            FunctionDescriptor sDesc = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS);
            MethodHandle sHandle = NiceBindings.g_object_set_handle(sDesc);
            if (sHandle != null) {
                sHandle.invokeExact(agentHandle, cStunServer, cServer, MemorySegment.NULL);
            }

            // int property
            MemorySegment cStunPort = localArena.allocateFrom("stun-server-port");
            FunctionDescriptor iDesc = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS);
            MethodHandle iHandle = NiceBindings.g_object_set_handle(iDesc);
            if (iHandle != null) {
                iHandle.invokeExact(agentHandle, cStunPort, port, MemorySegment.NULL);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Sets the controlling mode of the agent.
     * 
     * @param controlling True for controlling, false for controlled.
     */
    public void setControllingMode(boolean controlling) {
        try (var localArena = Arena.ofConfined()) {
            MemorySegment cProp = localArena.allocateFrom("controlling-mode");
            // specialized handle for boolean (int) property: g_object_set(obj, name, int, NULL)
            FunctionDescriptor desc = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS);
            MethodHandle handle = NiceBindings.g_object_set_handle(desc);
            if (handle != null) {
                handle.invokeExact(agentHandle, cProp, controlling ? 1 : 0, MemorySegment.NULL);
            }
            System.out.println("Property set: controlling-mode=" + controlling);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Disables ICE-TCP and UPnP for faster gathering in this demo.
     */
    public void disableExtraFeatures() {
        try (var localArena = Arena.ofConfined()) {
            FunctionDescriptor desc = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS);
            MethodHandle handle = NiceBindings.g_object_set_handle(desc);
            if (handle != null) {
                handle.invokeExact(agentHandle, localArena.allocateFrom("ice-tcp"), 0, MemorySegment.NULL);
                handle.invokeExact(agentHandle, localArena.allocateFrom("upnp"), 0, MemorySegment.NULL);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Generates a local SDP string.
     * 
     * @return The local SDP string.
     */
    public String generateLocalSdp() {
        try {
            if (NiceBindings.nice_agent_generate_local_sdp != null) {
                MemorySegment sdpPtr = (MemorySegment) NiceBindings.nice_agent_generate_local_sdp.invokeExact(agentHandle);
                if (sdpPtr.equals(MemorySegment.NULL)) return "";
                String sdp = sdpPtr.reinterpret(Long.MAX_VALUE).getString(0);
                try {
                    if (NiceBindings.g_free != null) {
                        NiceBindings.g_free.invokeExact(sdpPtr);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return sdp;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return "";
    }

    /**
     * Parses a remote SDP string.
     * 
     * @param sdp The remote SDP string to parse.
     * @return 0 on success, or a negative value on error.
     */
    public int parseRemoteSdp(String sdp) {
        try (var localArena = Arena.ofConfined()) {
            MemorySegment cSdp = localArena.allocateFrom(sdp);
            if (NiceBindings.nice_agent_parse_remote_sdp != null) {
                return (int) NiceBindings.nice_agent_parse_remote_sdp.invokeExact(agentHandle, cSdp);
            }
            return 0;
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    @Override
    public void close() {
        if (agentHandle != null && !agentHandle.equals(MemorySegment.NULL)) {
            try {
                if (NiceBindings.g_object_unref != null) {
                    NiceBindings.g_object_unref.invokeExact(agentHandle);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        arena.close();
    }

    public MemorySegment getHandle() {
        return agentHandle;
    }

    /**
     * Connects a signal to the agent.
     * 
     * @param signalName The signal name (e.g., "candidate-gathering-done").
     * @param callback The callback memory segment (created via Linker upcall).
     * @param data Optional user data.
     * @return The signal handler ID.
     */
    public long connectSignal(String signalName, MemorySegment callback, MemorySegment data) {
        try (var localArena = Arena.ofConfined()) {
            MemorySegment cSignal = localArena.allocateFrom(signalName);
            if (NiceBindings.g_signal_connect_data != null) {
                return (long) NiceBindings.g_signal_connect_data.invokeExact(agentHandle, cSignal, callback, data, MemorySegment.NULL, 0);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return 0;
    }

    /**
     * Attaches a receiver to a stream component.
     * 
     * @param streamId The stream ID.
     * @param componentId The component ID.
     * @param context The GLib main context (can be null for thread-default).
     * @param callback The receiver callback (created via Linker upcall).
     * @param data Optional user data.
     */
    public void attachReceiver(int streamId, int componentId, MemorySegment context, MemorySegment callback, MemorySegment data) {
        try {
            if (NiceBindings.nice_agent_attach_recv != null) {
                NiceBindings.nice_agent_attach_recv.invokeExact(agentHandle, streamId, componentId, context, callback, data);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Sends data over a stream component.
     * 
     * @param streamId The stream ID.
     * @param componentId The component ID.
     * @param data The data to send.
     * @return Number of bytes sent, or negative on error.
     */
    public int send(int streamId, int componentId, byte[] data) {
        try (var localArena = Arena.ofConfined()) {
            MemorySegment buf = localArena.allocateFrom(ValueLayout.JAVA_BYTE, data);
            if (NiceBindings.nice_agent_send != null) {
                return (int) NiceBindings.nice_agent_send.invokeExact(agentHandle, streamId, componentId, (int)data.length, buf);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return -1;
    }

    /**
     * Gets the list of local candidates for a component.
     * 
     * @param streamId The stream ID.
     * @param componentId The component ID.
     * @return A list of NiceCandidate objects.
     */
    public java.util.List<NiceCandidate> getLocalCandidates(int streamId, int componentId) {
        return getCandidates(NiceBindings.nice_agent_get_local_candidates, streamId, componentId);
    }

    /**
     * Gets the list of remote candidates for a component.
     * 
     * @param streamId The stream ID.
     * @param componentId The component ID.
     * @return A list of NiceCandidate objects.
     */
    public java.util.List<NiceCandidate> getRemoteCandidates(int streamId, int componentId) {
        return getCandidates(NiceBindings.nice_agent_get_remote_candidates, streamId, componentId);
    }

    private java.util.List<NiceCandidate> getCandidates(MethodHandle method, int streamId, int componentId) {
        java.util.List<NiceCandidate> candidates = new java.util.ArrayList<>();
        if (method == null) return candidates;

        try {
            MemorySegment listPtr = (MemorySegment) method.invokeExact(agentHandle, streamId, componentId);
            MemorySegment current = listPtr;
            
            while (current != null && !current.equals(MemorySegment.NULL)) {
                MemorySegment candidatePtr = (MemorySegment) NiceBindings.GSLIST_DATA.get(current.reinterpret(NiceBindings.GSLIST_LAYOUT.byteSize()), 0L);
                if (candidatePtr != null && !candidatePtr.equals(MemorySegment.NULL)) {

                    candidates.add(new NiceCandidate(candidatePtr.reinterpret(NiceBindings.NICE_CANDIDATE_LAYOUT.byteSize())));
                }
                current = (MemorySegment) NiceBindings.GSLIST_NEXT.get(current.reinterpret(NiceBindings.GSLIST_LAYOUT.byteSize()), 0L);
            }


            if (listPtr != null && !listPtr.equals(MemorySegment.NULL) && NiceBindings.g_slist_free != null) {
                NiceBindings.g_slist_free.invokeExact(listPtr);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return candidates;
    }
}

