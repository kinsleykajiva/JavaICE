package demo.io.github.kinsleykajiva;

import java.util.List;

import io.github.kinsleykajiva.ice.NiceAgent;
import io.github.kinsleykajiva.ice.NiceBindings;
import io.github.kinsleykajiva.ice.NiceCandidate;
import io.github.kinsleykajiva.ice.NiceStream;

/**
 * Advanced demo showcasing candidate inspection and stream state management.
 */
public class AdvancedIceDemo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting Advanced libnice Demo...");

        try (NiceAgent agent = new NiceAgent(java.lang.foreign.MemorySegment.NULL, NiceBindings.NICE_COMPATIBILITY_RFC5245)) {
            agent.setControllingMode(true);
            int streamId = agent.addStream(1);
            
            System.out.println("Gathering candidates for stream " + streamId + "...");
            agent.gatherCandidates(streamId);

            // Wait a bit for candidates to be gathered
            Thread.sleep(2000);

            // Accessing Advanced Features
            System.out.println("\n--- Local Candidates ---");
            List<NiceCandidate> localCandidates = agent.getLocalCandidates(streamId, 1);
            if (localCandidates.isEmpty()) {
                System.out.println("No local candidates gathered yet.");
            } else {
                for (NiceCandidate c : localCandidates) {
                    String transport = switch(c.getTransport()) {
                        case NiceBindings.NICE_CANDIDATE_TRANSPORT_UDP -> "UDP";
                        case NiceBindings.NICE_CANDIDATE_TRANSPORT_TCP_PASSIVE -> "TCP-PASSIVE";
                        case NiceBindings.NICE_CANDIDATE_TRANSPORT_TCP_ACTIVE -> "TCP-ACTIVE";
                        case NiceBindings.NICE_CANDIDATE_TRANSPORT_TCP_SO -> "TCP-SO";
                        default -> "Unknown";
                    };
                    System.out.printf("  [%s] %s:%d (Type: %d, Transport: %s, Foundation: %s)\n", 
                        c.getType() == 0 ? "Host" : "Other",
                        c.getAddress(), c.getPort(), 
                        c.getType(), transport, c.getFoundation());
                }

            }

            System.out.println("\n--- Stream Status ---");
            NiceStream stream = new NiceStream(agent, streamId);
            int state = stream.getComponentState(1);
            System.out.println("  Component 1 state ID: " + state);
            
            String stateStr = switch(state) {
                case NiceBindings.NICE_COMPONENT_STATE_DISCONNECTED -> "Disconnected";
                case NiceBindings.NICE_COMPONENT_STATE_GATHERING -> "Gathering";
                case NiceBindings.NICE_COMPONENT_STATE_CONNECTING -> "Connecting";
                case NiceBindings.NICE_COMPONENT_STATE_CONNECTED -> "Connected";
                case NiceBindings.NICE_COMPONENT_STATE_READY -> "Ready";
                case NiceBindings.NICE_COMPONENT_STATE_FAILED -> "Failed";
                default -> "Unknown";
            };
            System.out.println("  Component 1 state: " + stateStr);

           
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
