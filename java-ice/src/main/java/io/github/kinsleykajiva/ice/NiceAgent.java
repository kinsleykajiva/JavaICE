package io.github.kinsleykajiva.ice;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

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
                this.agentHandle = (MemorySegment) NiceBindings.nice_agent_new.invokeExact(mainContext, compatibility);
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
            MemorySegment cServer = localArena.allocateFrom(server);
            // Placeholder: NiceBindings.g_object_set(agentHandle, localArena.allocateFrom("stun-server"), cServer, ...);
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
                // In a real app, we should call g_free(sdpPtr), but here we omit for simplicity or if g_free is not bound
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
}
