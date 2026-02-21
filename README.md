# JavaICE: Java Wrappers for libnice

![Java Version](https://img.shields.io/badge/Java-22%2B-orange)
![License](https://img.shields.io/badge/License-MIT-blue)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kinsleykajiva/java-ice.svg)](https://search.maven.org/artifact/io.github.kinsleykajiva/java-ice)
![Topic](https://img.shields.io/badge/Topic-Networking-green)
![ICE](https://img.shields.io/badge/Protocol-ICE-brightgreen)


JavaICE provides high-level Java wrappers for [libnice](https://github.com/libnice/libnice), the GLib-based ICE (Interactive Connectivity Establishment) implementation. Leveraging **Project Panama** (Java 22+), it enables robust peer-to-peer connectivity and NAT traversal directly from Java.

---

## 🚀 Demos

Explore the capabilities of JavaICE with these prepared demos:
- **[SdpDemo](file:///c:/Users/Kinsley/IdeaProjects/JavaICE/demo/src/main/java/demo/io/github/kinsleykajiva/SdpDemo.java)**: Comprehensive example of agent initialization, candidate gathering, and SDP generation/parsing.
- **[ThreadedIceDemo](file:///c:/Users/Kinsley/IdeaProjects/JavaICE/demo/src/main/java/demo/io/github/kinsleykajiva/ThreadedIceDemo.java)**: Multi-threaded simulation of two agents exchanging candidates and establishing connectivity.

---

## Features

- **Project Panama Integration**: Direct interface with `libnice` without JNI headers.
- **Cross-Platform**: Supports Windows (`.dll`) and Linux (`.so`) native libraries.
- **NAT Traversal**: Full support for STUN and TURN via libnice.
- **Standards Compliant**: Implements modern ICE, Trickle ICE, and SDP specifications.

---

## Quick Start

### Maven Dependency

Add JavaICE to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.kinsleykajiva</groupId>
    <artifactId>java-ice</artifactId>
    <version>0.1.0</version>
</dependency>


```

### Basic Usage

```java
NiceAgent agent = new NiceAgent(false); // Create non-controlling agent
int streamId = agent.addStream(1);
agent.gatherCandidates();

// Generate Local SDP to share with peer
String localSdp = agent.generateLocalSdp();
System.out.println("Local SDP:\n" + localSdp);

// Parse Remote SDP from peer
agent.parseRemoteSdp(remoteSdp);
```

### 🛠️ Advanced Use

For fine-grained control, you can interact with candidates and streams directly:

```java
// List local candidates
List<NiceCandidate> candidates = agent.getLocalCandidates(streamId, 1);
for (NiceCandidate c : candidates) {
    System.out.printf("Candidate: %s:%d (Type: %d, Foundation: %s)\n", 
        c.getAddress(), c.getPort(), c.getType(), c.getFoundation());
}

// Check stream component state
NiceStream stream = new NiceStream(agent, streamId);
int state = stream.getComponentState(1);
System.out.println("Component 1 state: " + state);
```


---

## How It Works

### 1. Native Library Loading
The `NativeLibraryLoader` extracts the appropriate native library from the JAR's resources (Windows DLL or Linux SO) to a temporary folder and loads it using `SymbolLookup.libraryLookup`.

### 2. ICE Agent Initialization
The `NiceAgent` class wraps the native `NiceAgent` object and handles:
- Setting the controlling mode.
- Adding streams and components.
- Managing the GLib main loop (required for asynchronous operations).

### 3. SDP Exchange & Connectivity
Standard ICE flow is supported: Candidate gathering, SDP generation, and remote SDP parsing to establish a P2P connection.

---

## Standards & Technical Specifications

JavaICE (via libnice) implements a comprehensive suite of IETF standards:

- **ICE**: [RFC 8445](https://tools.ietf.org/html/rfc8445), [RFC 5245](https://tools.ietf.org/html/rfc5245)
- **Trickle ICE**: [RFC 8838](https://tools.ietf.org/html/rfc8838)
- **STUN/TURN**: [RFC 5389](https://tools.ietf.org/html/rfc5389), [RFC 5766](https://tools.ietf.org/html/rfc5766)
- **SDP**: [RFC 4566](https://tools.ietf.org/html/rfc4566), [RFC 8866](https://tools.ietf.org/html/rfc8866)

---

## Examples & Run Logs

### 1. SDP Demo (`SdpDemo`)
Initializes an agent, gathers candidates, and generates/parses SDP.

```text
Starting libnice SDP Demo...
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

### 2. Multi-threaded ICE Demo (`ThreadedIceDemo`)
Simulates two agents (`AgentA` and `AgentB`) exchanging SDP and candidates in separate threads.

```text
--- Multi-threaded ICE Demo Starting ---
[AgentA] Thread started. Initializing agent...
[AgentB] Thread started. Initializing agent...
[AgentA] Added stream 1. Gathering candidates... Success: true
[AgentB] Added stream 1. Gathering candidates... Success: true
[AgentB] Local SDP generated:
m=- 59252 ICE/SDP
...
[AgentA] Received remote SDP. Parsing...
[AgentB] Received remote SDP. Parsing...
--- Multi-threaded ICE Demo Finished ---
```

### 3. Advanced Demo (`AdvancedIceDemo`)
Directly inspects gathered candidates and monitors component states using high-level wrappers.

```text
Starting Advanced libnice Demo...
Gathering candidates for stream 1...

--- Local Candidates ---
  [Host] 192.168.0.101:59685 (Type: 0, Transport: UDP, Foundation: 1)
  [Host] 192.168.0.101:0 (Type: 0, Transport: TCP-PASSIVE, Foundation: 2)
  [Host] 192.168.0.101:65502 (Type: 0, Transport: TCP-ACTIVE, Foundation: 3)

--- Stream Status ---
  Component 1 state ID: 1
  Component 1 state: Gathering

Demo completed.
```


---

## Native Build Guide (libnice on Windows)

This section explains how to build **libnice** as a Windows `.dll` for use with JavaICE.

### 1. Prerequisites (MSYS2)
1. Download and install [MSYS2](https://www.msys2.org/).
2. Open **MSYS2 MinGW64** (Do NOT use the plain MSYS shell).
3. Update and install toolchain:
   ```bash
   pacman -Syu
   pacman -S mingw-w64-x86_64-toolchain mingw-w64-x86_64-meson mingw-w64-x86_64-ninja mingw-w64-x86_64-glib2 git
   ```

### 2. Build Steps
```bash
git clone https://github.com/libnice/libnice.git
cd libnice
meson setup build --buildtype=release -Dtests=disabled -Dexamples=disabled
meson compile -C build
```
The generated DLL will be found at `build/nice/libnice-10.dll`.

---
