package io.github.kinsleykajiva.ice;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Utility to load native libraries from the classpath.
 * It extracts the appropriate library for the current OS/Arch to a temporary directory.
 */
public class NativeLibraryLoader {

    public static SymbolLookup loadLibrary(String libBaseName) {
        String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
	    
	    
	    String platform;
        String extension;
        String prefix = "";

        if (os.contains("win")) {
            platform = "windows-x64";
            extension = ".dll";
        } else if (os.contains("linux")) {
            platform = "linux-x64";
            extension = ".so.10";
            prefix = "lib";
        } else {
            throw new RuntimeException("Unsupported OS: " + os);
        }

        // Adjust libName based on OS conventions if necessary
        String libName = prefix + libBaseName;
        if (libBaseName.equals("nice")) {
             if (os.contains("win")) {
                 libName = "libnice-10";
             } else {
                 libName = "libnice";
             }
        }

        String resourcePath = "/natives/" + platform + "/" + libName + extension;
        System.out.println("Attempting to load native library from resource: " + resourcePath);
        
        try (InputStream is = NativeLibraryLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                // Try to find ANY libnice.so* if exact match fails for Linux
                if (os.contains("linux")) {
                    // This is harder since we can't easily list resources in a JAR/classpath directory.
                    // But we can try common version strings or just the base .so
                    String[] fallbacks = {"/natives/" + platform + "/libnice.so.10.15.0", "/natives/" + platform + "/libnice.so"};
                    for (String fallback : fallbacks) {
                        InputStream fis = NativeLibraryLoader.class.getResourceAsStream(fallback);
                        if (fis != null) {
                            System.out.println("Found fallback native library: " + fallback);
                            return extractAndLoad(fis, libName, extension, fallback);
                        }
                    }
                }

                // Try searching without leading slash if using classloader directly
                InputStream is2 = NativeLibraryLoader.class.getClassLoader().getResourceAsStream(resourcePath.substring(1));
                if (is2 != null) {
                    return extractAndLoad(is2, libName, extension, resourcePath);
                }
                throw new IOException("Native library not found in classpath: " + resourcePath);
            }

            return extractAndLoad(is, libName, extension, resourcePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load native library: " + resourcePath + ". Error: " + e.getMessage(), e);
        }
    }

    private static SymbolLookup extractAndLoad(InputStream is, String libName, String extension, String resourcePath) throws IOException {
        Path tempDir = Files.createTempDirectory("java-ice-natives-");
        tempDir.toFile().deleteOnExit();
        
        Path tempFile = tempDir.resolve(libName + extension);
        Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
        tempFile.toFile().deleteOnExit();

        System.out.println("Extracted native library from " + resourcePath + " to: " + tempFile);
        
        return SymbolLookup.libraryLookup(tempFile, Arena.global());
    }
}
