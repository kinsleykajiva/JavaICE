package demo.io.github.kinsleykajiva;

import io.github.kinsleykajiva.ice.GLibContext;
import io.github.kinsleykajiva.ice.NiceAgent;
import io.github.kinsleykajiva.ice.NiceBindings;

/**
 * Demo application showing libnice SDP generation and parsing.
 */
public class SdpDemo {
    public static void main(String[] args) {
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("demo_started.txt"), "Demo started at " + java.time.LocalDateTime.now());
        } catch (Exception e) {}
        System.out.println("Starting libnice SDP Demo...");

        try (GLibContext glib = new GLibContext();
             NiceAgent agent = new NiceAgent(glib.getContext(), NiceBindings.NICE_COMPATIBILITY_RFC5245)) {

            glib.start();

            int streamId = agent.addStream(1);
            System.out.println("Added stream: " + streamId);

            System.out.println("Gathering candidates...");
            agent.gatherCandidates(streamId);
            
            // Wait for some candidates to be gathered
            System.out.println("Waiting for candidates (5s)...");
            Thread.sleep(5000);

            String localSdp = agent.generateLocalSdp();
            System.out.println("Generated Local SDP:");
            System.out.println("-------------------");
            System.out.println(localSdp.isEmpty() ? "[Empty SDP - native bindings not yet linked]" : localSdp);
            System.out.println("-------------------");

            // Mocking a remote SDP exchange
            String remoteSdp = localSdp; // In a real app, this comes from the peer via signaling
            if (!remoteSdp.isEmpty()) {
                int result = agent.parseRemoteSdp(remoteSdp);
                System.out.println("Parsed remote SDP, result: " + result);
            }

            glib.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
