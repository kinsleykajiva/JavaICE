package demo.io.github.kinsleykajiva;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import io.github.kinsleykajiva.ice.NiceAgent;
import io.github.kinsleykajiva.ice.NiceBindings;

/**
 * Utility to dump the raw memory of a candidate to help debug struct alignment.
 */
public class MemoryInspector {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting Memory Inspector...");

        try (NiceAgent agent = new NiceAgent(java.lang.foreign.MemorySegment.NULL, NiceBindings.NICE_COMPATIBILITY_RFC5245)) {
            int streamId = agent.addStream(1);
            agent.gatherCandidates(streamId);
            Thread.sleep(1000);

            var candidates = agent.getLocalCandidates(streamId, 1);
            if (!candidates.isEmpty()) {
                MemorySegment handle = candidates.get(0).getHandle();
                System.out.println("Dumping first candidate memory (256 bytes):");
                dumpMemory(handle, 256);
            } else {
                System.out.println("No candidates gathered. Try running again or check STUN/Network.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void dumpMemory(MemorySegment segment, long size) {
        for (long i = 0; i < size; i++) {
            if (i > 0 && i % 16 == 0) System.out.println();
            if (i % 8 == 0) System.out.print("  ");
            
            byte b = segment.get(ValueLayout.JAVA_BYTE, i);
            System.out.printf("%02X ", b);
        }
        System.out.println();
    }
}
