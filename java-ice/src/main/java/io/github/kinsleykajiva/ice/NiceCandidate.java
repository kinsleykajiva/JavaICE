package io.github.kinsleykajiva.ice;

import java.lang.foreign.Arena;
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
        if (handle == null || handle.equals(MemorySegment.NULL)) return "";
        // Foundation is a fixed-size char array (33 bytes)
        MemorySegment foundationSlice = handle.asSlice(NiceBindings.NICE_CANDIDATE_LAYOUT.byteOffset(java.lang.foreign.MemoryLayout.PathElement.groupElement("foundation")), 33);
        return foundationSlice.reinterpret(33).getString(0);
    }

    public int getType() {
        if (handle == null || handle.equals(MemorySegment.NULL)) return 0;
        return (int) NiceBindings.CANDIDATE_TYPE.get(handle, 0L);
    }

    public int getTransport() {
        if (handle == null || handle.equals(MemorySegment.NULL)) return 0;
        return (int) NiceBindings.CANDIDATE_TRANSPORT.get(handle, 0L);
    }


    public String getAddress() {
        if (handle == null || handle.equals(MemorySegment.NULL)) return "";
        try (var arena = Arena.ofConfined()) {
            MemorySegment addrPtr = handle.asSlice(NiceBindings.NICE_CANDIDATE_LAYOUT.byteOffset(java.lang.foreign.MemoryLayout.PathElement.groupElement("addr")), NiceBindings.NICE_ADDRESS_LAYOUT.byteSize());
            MemorySegment buf = arena.allocate(256); // NiceAddress string buffer
            if (NiceBindings.nice_address_to_string != null) {
                NiceBindings.nice_address_to_string.invokeExact(addrPtr, buf);
                return buf.reinterpret(256).getString(0);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return "";
    }

    public int getPort() {
        if (handle == null || handle.equals(MemorySegment.NULL)) return 0;
        try {
            MemorySegment addrPtr = handle.asSlice(NiceBindings.NICE_CANDIDATE_LAYOUT.byteOffset(java.lang.foreign.MemoryLayout.PathElement.groupElement("addr")), NiceBindings.NICE_ADDRESS_LAYOUT.byteSize());
            if (NiceBindings.nice_address_get_port != null) {
                return (int) NiceBindings.nice_address_get_port.invokeExact(addrPtr);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return 0;
    }

    public MemorySegment getHandle() {
        return handle;
    }
}

