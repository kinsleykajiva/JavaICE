package io.github.kinsleykajiva.ice;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * Low-level bindings container.
 * This class will be populated by jextract or manual MethodHandles.
 * It uses Linker.Option.critical for performance-sensitive native calls.
 */
public class NiceBindings {
    private static final Linker LINKER = Linker.nativeLinker();
    static {
        SymbolLookup lookup = null;
        String[] libNames = {"libnice-10", "nice-10", "libnice"};
//        String customPath = "./libnice/build-x-win64/nice/libnice-10.dll"; // windows
        String customPath = "./libnice/build-x-linux/nice/libnice.so.10"; // linux

        if (customPath != null) {
            try {
                lookup = SymbolLookup.libraryLookup(java.nio.file.Path.of(customPath), Arena.global());
                System.out.println("libnice loaded from custom path: " + customPath);
            } catch (Exception e) {
                System.err.println("Failed to load libnice from custom path: " + customPath);
                e.printStackTrace();
            }
        }

        if (lookup == null) {
            for (String name : libNames) {
                try {
                    lookup = SymbolLookup.libraryLookup(name, Arena.global());
                    System.out.println("libnice loaded as: " + name);
                    break;
                } catch (Exception e) {
                    // Try next name
                }
            }
        }

        if (lookup == null) {
            System.err.println("Warning: libnice not found in system path or custom path.");
            System.err.println("Actual library search carried out for: " + String.join(", ", libNames));
            if (customPath != null) {
                System.err.println("Custom path tried: " + customPath);
            }
            System.err.println("Please ensure libnice-10.dll (or equivalent) and its dependencies (GLib-2.0, etc.) are in your PATH");
            System.err.println("or specify its location with -Dice.lib.path=C:\\path\\to\\libnice-10.dll");
        }

        final SymbolLookup finalLookup = lookup;

        // Function descriptor for nice_agent_send
        FunctionDescriptor descriptor = FunctionDescriptor.of(
            ValueLayout.JAVA_INT,      // return value (ssize_t)
            ValueLayout.ADDRESS,       // NiceAgent* agent
            ValueLayout.JAVA_INT,      // stream_id
            ValueLayout.JAVA_INT,      // component_id
            ValueLayout.JAVA_INT,      // len
            ValueLayout.ADDRESS        // buf
        );

        nice_agent_send = findHandle(finalLookup, "nice_agent_send", descriptor, Linker.Option.critical(true));
        nice_agent_generate_local_sdp = findHandle(finalLookup, "nice_agent_generate_local_sdp", 
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        nice_agent_parse_remote_sdp = findHandle(finalLookup, "nice_agent_parse_remote_sdp", 
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        
        nice_agent_new = findHandle(finalLookup, "nice_agent_new",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        nice_agent_add_stream = findHandle(finalLookup, "nice_agent_add_stream",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        nice_agent_gather_candidates = findHandle(finalLookup, "nice_agent_gather_candidates",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

        // GLib symbols usually require libglib-2.0-0, but sometimes they are exported or aliased
        g_main_context_new = findHandle(finalLookup, "g_main_context_new", FunctionDescriptor.of(ValueLayout.ADDRESS));
        g_main_loop_new = findHandle(finalLookup, "g_main_loop_new", 
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        g_main_loop_run = findHandle(finalLookup, "g_main_loop_run", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        g_main_loop_quit = findHandle(finalLookup, "g_main_loop_quit", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        g_object_unref = findHandle(finalLookup, "g_object_unref", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        g_main_context_unref = findHandle(finalLookup, "g_main_context_unref", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        g_main_loop_unref = findHandle(finalLookup, "g_main_loop_unref", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        g_free = findHandle(finalLookup, "g_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
    }

    private static MethodHandle findHandle(SymbolLookup lookup, String name, FunctionDescriptor desc, Linker.Option... options) {
        if (lookup == null) return null;
        return lookup.find(name).map(addr -> LINKER.downcallHandle(addr, desc, options)).orElse(null);
    }

    public static final MethodHandle nice_agent_send;
    public static final MethodHandle nice_agent_generate_local_sdp;
    public static final MethodHandle nice_agent_parse_remote_sdp;
    public static final MethodHandle nice_agent_new;
    public static final MethodHandle nice_agent_add_stream;
    public static final MethodHandle nice_agent_gather_candidates;

    public static final MethodHandle g_main_context_new;
    public static final MethodHandle g_main_loop_new;
    public static final MethodHandle g_main_loop_run;
    public static final MethodHandle g_main_loop_quit;
    public static final MethodHandle g_object_unref;
    public static final MethodHandle g_main_context_unref;
    public static final MethodHandle g_main_loop_unref;
    public static final MethodHandle g_free;

    // Nice compatibility modes
    public static final int NICE_COMPATIBILITY_RFC5245 = 0;
    public static final int NICE_COMPATIBILITY_DRAFT19 = 1;
    public static final int NICE_COMPATIBILITY_GOOGLE = 2;
    public static final int NICE_COMPATIBILITY_MSN = 3;
}
