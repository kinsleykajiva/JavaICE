package io.github.kinsleykajiva.ice;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

/**
 * Low-level bindings container.
 * This class will be populated by jextract or manual MethodHandles.
 * It uses Linker.Option.critical for performance-sensitive native calls.
 */
public class NiceBindings {
    private static final Linker LINKER = Linker.nativeLinker();
    static {
        SymbolLookup lookup = null;
        try {
            lookup = NativeLibraryLoader.loadLibrary("nice");
        } catch (Exception e) {
            System.err.println("Failed to load libnice using NativeLibraryLoader: " + e.getMessage());
        }

        if (lookup == null) {
            String[] libNames = {"libnice-10", "nice-10", "libnice"};
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
            System.err.println("Warning: libnice not found in system path or resources.Please review the ReadMe file of this project for instructions on how to provide the native library. Native bindings will not be functional.");
        }

        final SymbolLookup finalLookup = lookup;

        // Initialize networking (important for libnice on some platforms)
        try {
            MethodHandle netInit = findHandle(finalLookup, "g_networking_init", FunctionDescriptor.ofVoid());
            if (netInit != null) {
                netInit.invokeExact();
                System.out.println("GLib networking initialized.");
            }
        } catch (Throwable t) {
            System.err.println("Warning: Failed to initialize GLib networking.");
        }

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

        g_networking_init = findHandle(finalLookup, "g_networking_init", FunctionDescriptor.ofVoid());
        g_object_set = findHandle(finalLookup, "g_object_set", FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // object
            ValueLayout.ADDRESS  // first_property_name
            // ... variadic (we will use a simplified version for common properties)
        ));

        g_main_context_push_thread_default = findHandle(finalLookup, "g_main_context_push_thread_default", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        g_main_context_pop_thread_default = findHandle(finalLookup, "g_main_context_pop_thread_default", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        
        g_signal_connect_data = findHandle(finalLookup, "g_signal_connect_data", FunctionDescriptor.of(ValueLayout.JAVA_LONG, 
            ValueLayout.ADDRESS, // instance
            ValueLayout.ADDRESS, // detailed_signal
            ValueLayout.ADDRESS, // c_handler
            ValueLayout.ADDRESS, // data
            ValueLayout.ADDRESS, // destroy_data
            ValueLayout.JAVA_INT // connect_flags
        ));

        nice_agent_attach_recv = findHandle(finalLookup, "nice_agent_attach_recv", FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // agent
            ValueLayout.JAVA_INT, // stream_id
            ValueLayout.JAVA_INT, // component_id
            ValueLayout.ADDRESS, // ctx
            ValueLayout.ADDRESS, // func
            ValueLayout.ADDRESS  // data
        ));

        nice_agent_get_component_state = findHandle(finalLookup, "nice_agent_get_component_state",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        
        nice_agent_get_local_candidates = findHandle(finalLookup, "nice_agent_get_local_candidates",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        
        nice_agent_get_remote_candidates = findHandle(finalLookup, "nice_agent_get_remote_candidates",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

        nice_address_to_string = findHandle(finalLookup, "nice_address_to_string",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        
        nice_address_get_port = findHandle(finalLookup, "nice_address_get_port",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

        g_slist_free = findHandle(finalLookup, "g_slist_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
    }




    public static MethodHandle g_object_set_handle(FunctionDescriptor desc) {
        if (g_object_set_addr == null) return null;
        return LINKER.downcallHandle(g_object_set_addr, desc);
    }

    private static MemorySegment g_object_set_addr = null;

    private static MethodHandle findHandle(SymbolLookup lookup, String name, FunctionDescriptor desc, Linker.Option... options) {
        if (lookup == null) return null;
        if (name.equals("g_object_set")) {
            g_object_set_addr = lookup.find(name).orElse(null);
        }
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
    public static final MethodHandle g_networking_init;
    public static final MethodHandle g_object_set;

    public static final MethodHandle g_main_context_push_thread_default;
    public static final MethodHandle g_main_context_pop_thread_default;
    public static final MethodHandle g_signal_connect_data;
    public static final MethodHandle nice_agent_attach_recv;
    public static final MethodHandle nice_agent_get_component_state;
    public static final MethodHandle nice_agent_get_local_candidates;
    public static final MethodHandle nice_agent_get_remote_candidates;
    public static final MethodHandle nice_address_to_string;
    public static final MethodHandle nice_address_get_port;
    public static final MethodHandle g_slist_free;

    // Struct Layouts
    public static final StructLayout GSLIST_LAYOUT = MemoryLayout.structLayout(
        ValueLayout.ADDRESS.withName("data"),
        ValueLayout.ADDRESS.withName("next")
    ).withName("GSList");

    // VarHandles for GSList
    public static final VarHandle GSLIST_DATA = GSLIST_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("data"));
    public static final VarHandle GSLIST_NEXT = GSLIST_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("next"));

    // Simple NiceAddress (opaque for now, but we'll provide helper to stringify)
    // Sized to match sockaddr_in6 (28 bytes) which is the largest candidate address type.
    public static final StructLayout NICE_ADDRESS_LAYOUT = MemoryLayout.structLayout(
        MemoryLayout.sequenceLayout(28, ValueLayout.JAVA_BYTE).withName("opaque")
    ).withName("NiceAddress");



    public static final StructLayout NICE_CANDIDATE_LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_INT.withName("type"),
        ValueLayout.JAVA_INT.withName("transport"),
        NICE_ADDRESS_LAYOUT.withName("addr"),
        NICE_ADDRESS_LAYOUT.withName("base_addr"),
        ValueLayout.JAVA_INT.withName("priority"),
        ValueLayout.JAVA_INT.withName("stream_id"),
        ValueLayout.JAVA_INT.withName("component_id"),
        MemoryLayout.sequenceLayout(33, ValueLayout.JAVA_BYTE).withName("foundation"),
        MemoryLayout.paddingLayout(3), // alignment
        ValueLayout.ADDRESS.withName("username"),
        ValueLayout.ADDRESS.withName("password"),
        NICE_ADDRESS_LAYOUT.withName("turn_addr"),
        ValueLayout.JAVA_INT.withName("turn_transport"),
        MemoryLayout.paddingLayout(4) // alignment/extensibility
    ).withName("NiceCandidate");

    // VarHandles for NiceCandidate
    public static final VarHandle CANDIDATE_TYPE = NICE_CANDIDATE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("type"));
    public static final VarHandle CANDIDATE_TRANSPORT = NICE_CANDIDATE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("transport"));
    public static final VarHandle CANDIDATE_PRIORITY = NICE_CANDIDATE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("priority"));
    public static final VarHandle CANDIDATE_STREAM_ID = NICE_CANDIDATE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("stream_id"));
    public static final VarHandle CANDIDATE_COMPONENT_ID = NICE_CANDIDATE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("component_id"));
    
    // VarHandles for strings (addresses of strings)
    public static final VarHandle CANDIDATE_USERNAME = NICE_CANDIDATE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("username"));
    public static final VarHandle CANDIDATE_PASSWORD = NICE_CANDIDATE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("password"));

    // Nice compatibility modes
    public static final int NICE_COMPATIBILITY_RFC5245 = 0;
    public static final int NICE_COMPATIBILITY_DRAFT19 = 1;
    public static final int NICE_COMPATIBILITY_GOOGLE = 2;
    public static final int NICE_COMPATIBILITY_MSN = 3;

    // Nice component states
    public static final int NICE_COMPONENT_STATE_DISCONNECTED = 0;
    public static final int NICE_COMPONENT_STATE_GATHERING = 1;
    public static final int NICE_COMPONENT_STATE_CONNECTING = 2;
    public static final int NICE_COMPONENT_STATE_CONNECTED = 3;
    public static final int NICE_COMPONENT_STATE_READY = 4;
    public static final int NICE_COMPONENT_STATE_FAILED = 5;

    // Nice transport types
    public static final int NICE_CANDIDATE_TRANSPORT_UDP = 0;
    public static final int NICE_CANDIDATE_TRANSPORT_TCP_PASSIVE = 1;
    public static final int NICE_CANDIDATE_TRANSPORT_TCP_ACTIVE = 2;
    public static final int NICE_CANDIDATE_TRANSPORT_TCP_SO = 3;
}

