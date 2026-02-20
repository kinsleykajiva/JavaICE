package io.github.kinsleykajiva.ice;

import java.lang.foreign.MemorySegment;

/**
 * Represents a libnice stream.
 */
public class NiceStream {
    private final NiceAgent agent;
    private final int streamId;

    public NiceStream(NiceAgent agent, int streamId) {
        this.agent = agent;
        this.streamId = streamId;
    }

    public int getStreamId() {
        return streamId;
    }

    /**
     * Gets the current state of a component in the stream.
     * 
     * @param componentId The component ID.
     * @return The component state (NiceComponentState).
     */
    public int getComponentState(int componentId) {
        // Placeholder: return NiceBindings.nice_agent_get_component_state(agent.getHandle(), streamId, componentId);
        return 0;
    }

    /**
     * Sends data over a component.
     * 
     * @param componentId The component ID.
     * @param data The data to send.
     * @return Number of bytes sent.
     */
    public int send(int componentId, byte[] data) {
        // Use Linker.Option.critical if possible for this call in the low-level bindings.
        // Placeholder logic
        return data.length;
    }
}
