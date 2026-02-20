package io.github.kinsleykajiva.ice;

import java.lang.foreign.MemorySegment;

/**
 * Represents an ICE candidate.
 */
public class NiceCandidate {
    private final MemorySegment handle;

    public NiceCandidate(MemorySegment handle) {
        this.handle = handle;
    }

    public String getFoundation() {
        // Placeholder: return NiceBindings.NiceCandidate.foundation$get(handle).getUtf8String(0);
        return "";
    }

    public int getType() {
        // Placeholder: return NiceBindings.NiceCandidate.type$get(handle);
        return 0;
    }

    public String getAddress() {
        // Placeholder
        return "";
    }

    public int getPort() {
        // Placeholder
        return 0;
    }
}
