package demo.io.github.kinsleykajiva;

import io.github.kinsleykajiva.ice.GLibContext;
import io.github.kinsleykajiva.ice.NiceAgent;
import io.github.kinsleykajiva.ice.NiceBindings;

/**
 * Demo application showing libnice Java binding usage.
 */
public class IceDemo {
    public static void main(String[] args) {
        System.out.println("Starting libnice Java Demo...");

        try (GLibContext glib = new GLibContext();
             NiceAgent agent = new NiceAgent(glib.getContext(), NiceBindings.NICE_COMPATIBILITY_RFC5245)) {

            glib.start();

            int streamId = agent.addStream(1); // 1 component (e.g., RTP)
            System.out.println("Added stream with ID: " + streamId);

            agent.setStunServer("stun.l.google.com", 19302);
            
            System.out.println("Gathering candidates...");
            if (agent.gatherCandidates(streamId)) {
                System.out.println("Candidate gathering started.");
                // In a real app, you would wait for signals/callbacks here
                Thread.sleep(5000);
            } else {
                System.err.println("Failed to start candidate gathering.");
            }

            glib.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
