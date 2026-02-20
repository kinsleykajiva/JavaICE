# Building libnice on Windows (DLL)

This guide explains how to build **libnice** as a Windows `.dll` using the MinGW toolchain via MSYS2.

libnice is a GLib-based ICE (Interactive Connectivity Establishment) implementation maintained within the GNOME ecosystem.

Repository: [https://github.com/libnice/libnice.git](https://github.com/libnice/libnice.git)
Project: libnice
Toolchain provider: MSYS2

---

# How It Works

JavaICE leverages **Project Panama** (Java 22+) to interface directly with the `libnice` C library without needing JNI headers.

### 1. Native Library Loading
The `NativeLibraryLoader` extracts the appropriate DLL (Windows) or Shared Object (Linux) from the JAR's resources to a temporary folder and loads it using `SymbolLookup.libraryLookup`.

### 2. ICE Agent Initialization
The `NiceAgent` class wraps the native `NiceAgent` object. It handles:
- Setting the controlling mode.
- Adding streams and components.
- Managing the GLib main loop (required by libnice for asynchronous operations).

### 3. Candidate Gathering
When `gatherCandidates()` is called, libnice starts searching for local host addresses, reflexive (STUN), and relayed (TURN) addresses. The Java layer waits (or listens for signals) until gathering is complete.

### 4. SDP Generation
Once candidates are gathered, JavaICE can generate an **SDP (Session Description Protocol)** snippet. This snippet (containing `ice-ufrag`, `ice-pwd`, and `candidate` lines) can be sent to a remote peer.

### 5. SDP Exchange & Connectivity
- **Local SDP**: Your agent's connection info.
- **Remote SDP**: The other agent's connection info.
When you call `parseRemoteSdp()`, JavaICE feeds the remote candidates into the native `libnice` agent, which then begins the connectivity checks to establish a P2P connection.

---

# 1. Install MSYS2

1. Download MSYS2 from:
   [https://www.msys2.org/](https://www.msys2.org/)

2. Install it to the default location:

```
C:\msys64
```

3. After installation, open:

```
MSYS2 MinGW64
```

⚠️ Do NOT use the plain MSYS shell.
You must use **MinGW64** to produce a proper Windows DLL.

---

# 2. Update MSYS2

Inside the **MinGW64** shell:

```bash
pacman -Syu
```

If prompted to restart, close the shell and reopen **MSYS2 MinGW64**, then run:

```bash
pacman -Su
```

---

# 3. Install Required Toolchain & Dependencies

Install the full MinGW build stack:

```bash
pacman -S \
  mingw-w64-x86_64-toolchain \
  mingw-w64-x86_64-meson \
  mingw-w64-x86_64-ninja \
  mingw-w64-x86_64-glib2 \
  mingw-w64-x86_64-gstreamer \
  mingw-w64-x86_64-pkg-config \
  git
```

This installs:

* GCC (MinGW)
* Meson build system
* Ninja build backend
* GLib
* GStreamer (dependency)
* pkg-config
* Git

---

# 4. Clone libnice

Choose a working directory, for example:

```bash
cd /c/Users/YourUsername/IdeaProjects
```

Clone the repository:

```bash
git clone https://github.com/libnice/libnice.git
cd libnice
```

---

# 5. Configure the Build

Delete any previous build directory (if present):

```bash
rm -rf build
```

Run Meson setup:

```bash
meson setup build \
  --prefix=/mingw64 \
  --buildtype=release \
  -Dtests=disabled \
  -Dexamples=disabled \
  -Dgtk_doc=disabled
```

Explanation:

* `--prefix=/mingw64` ensures proper MinGW linking
* `-Dtests=disabled` avoids Linux-only `libdl` issues
* `release` build produces optimized DLL

If configuration completes successfully, continue.

---

# 6. Compile

```bash
meson compile -C build
```

This builds libnice as a Windows DLL.

---

# 7. Locate the Generated DLL

After successful compilation, the DLL will be located at:

```
build/nice/libnice-10.dll
```

If installed:

```bash
meson install -C build
```

The DLL will be placed in:

```
C:\msys64\mingw64\bin\libnice-10.dll
```

---

# 8. Using the DLL from Windows / Java

If using JNI, JNA, or Project Panama:

Add to your Windows PATH:

```
C:\msys64\mingw64\bin
```

Or temporarily in PowerShell:

```powershell
$env:PATH += ";C:\msys64\mingw64\bin"
```

---

# 9. Important Notes

* This build uses **MinGW GCC**.
* Do NOT mix this DLL with Microsoft Visual Studio (MSVC) compiled binaries.
* If you require MSVC compatibility, libnice and all dependencies must be built with MSVC.

---

# Summary

| Step | Action                          |
| ---- | ------------------------------- |
| 1    | Install MSYS2                   |
| 2    | Update pacman                   |
| 3    | Install MinGW toolchain         |
| 4    | Clone libnice                   |
| 5    | Configure Meson (disable tests) |
| 6    | Compile                         |
| 7    | Retrieve `libnice-10.dll`       |

---

# Examples & Run Logs

## SDP Demo (`SdpDemo`)
This demo initializes a libnice agent, gathers candidates, and generates a Local SDP. It also demonstrates parsing a remote SDP.

```text
Starting libnice SDP Demo...
Attempting to load native library from resource: /natives/windows-x64/libnice-10.dll
Extracted native library from /natives/windows-x64/libnice-10.dll to: C:\Users\Kinsley\AppData\Local\Temp\java-ice-natives-302889424220344620\libnice-10.dll
WARNING: A restricted method in java.lang.foreign.SymbolLookup has been called
...
Added stream: 1
Gathering candidates...
Waiting for candidates (5s)...
Generated Local SDP:
-------------------
m=- 55090 ICE/SDP
c=IN IP4 192.168.0.101
a=ice-ufrag:682A
a=ice-pwd:oHmR9Xh0wCuRmHiBYgboQn
a=candidate:1 1 UDP 2015363327 192.168.0.101 54001 typ host
...
-------------------
Parsed remote SDP, result: 27
```

## Multi-threaded ICE Demo (`ThreadedIceDemo`)
This demo simulates two agents (`AgentA` and `AgentB`) running in separate threads, gathering candidates independently.

```text
--- Multi-threaded ICE Demo Starting ---
[AgentA] Thread started. Initializing agent...
[AgentB] Thread started. Initializing agent...
Property set: controlling-mode=false
Property set: controlling-mode=true
[AgentA] Added stream 1. Gathering candidates... Success: true
[AgentB] Added stream 1. Gathering candidates... Success: true
...
[AgentB] Local SDP generated:
m=- 59252 ICE/SDP
c=IN IP4 192.168.0.101
a=ice-ufrag:Pmte
...
[AgentA] Local SDP generated:
m=- 59253 ICE/SDP
c=IN IP4 192.168.0.101
a=ice-ufrag:A6+X
...
[AgentA] Waiting for peer exchange...
[AgentB] Waiting for peer exchange...
[AgentB] Received remote SDP. Parsing...
[AgentA] Received remote SDP. Parsing...
Demo timed out after 60 seconds!
--- Multi-threaded ICE Demo Finished ---
```

