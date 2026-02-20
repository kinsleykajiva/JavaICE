package io.github.kinsleykajiva.ice;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;

/**
 * Utility to load native libraries from the classpath.
 * It extracts the appropriate library for the current OS/Arch to a temporary directory.
 */
public class NativeLibraryLoader {
    
    private record PlatformInfo(String platform, String extension, String prefix) {}
    
    public static SymbolLookup loadLibrary(String libBaseName) {
        String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        PlatformInfo platformInfo = resolvePlatformInfo(os);
        String libName = resolveLibName(libBaseName, os, platformInfo.prefix());
        String resourcePath = buildResourcePath(platformInfo.platform(), libName, platformInfo.extension());
        
        System.out.println("Attempting to load native library from resource: " + resourcePath);
        
        try {
            InputStream is = resolveInputStream(resourcePath, libName, platformInfo.platform(), os);
            return extractAndLoad(is, libName, platformInfo.extension(), resourcePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load native library: " + resourcePath + ". Error: " + e.getMessage(), e);
        }
    }
    
    private static PlatformInfo resolvePlatformInfo(String os) {
        if (os.contains("win")) {
            return new PlatformInfo("windows-x64", ".dll", "");
        } else if (os.contains("linux")) {
            return new PlatformInfo("linux-x64", ".so.10", "lib");
        }
        throw new RuntimeException("Unsupported OS: " + os);
    }
    
    private static String resolveLibName(String libBaseName, String os, String prefix) {
        if (!libBaseName.equals("nice")) {
            return prefix + libBaseName;
        }
        return os.contains("win") ? "libnice-10" : "libnice";
    }
    
    private static String buildResourcePath(String platform, String libName, String extension) {
        return "/natives/" + platform + "/" + libName + extension;
    }
    
    private static InputStream resolveInputStream(String resourcePath, String libName, String platform, String os) throws IOException {
        InputStream is = NativeLibraryLoader.class.getResourceAsStream(resourcePath);
        if (is != null) {
            return is;
        }
        
        if (os.contains("linux")) {
            Optional<InputStream> fallback = tryLinuxFallbacks(platform);
            if (fallback.isPresent()) {
                return fallback.get();
            }
        }
        
        InputStream classloaderStream = NativeLibraryLoader.class.getClassLoader()
                                                .getResourceAsStream(resourcePath.substring(1));
        if (classloaderStream != null) {
            return classloaderStream;
        }
        
        throw new IOException("Native library not found in classpath: " + resourcePath);
    }
    
    private static Optional<InputStream> tryLinuxFallbacks(String platform) {
        String[] fallbacks = {
                "/natives/" + platform + "/libnice.so.10.15.0",
                "/natives/" + platform + "/libnice.so"
        };
        for (String fallback : fallbacks) {
            InputStream fis = NativeLibraryLoader.class.getResourceAsStream(fallback);
            if (fis != null) {
                System.out.println("Found fallback native library: " + fallback);
                return Optional.of(fis);
            }
        }
        return Optional.empty();
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