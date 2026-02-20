# Building libnice on Windows (DLL)

This guide explains how to build **libnice** as a Windows `.dll` using the MinGW toolchain via MSYS2.

libnice is a GLib-based ICE (Interactive Connectivity Establishment) implementation maintained within the GNOME ecosystem.

Repository: [https://github.com/libnice/libnice.git](https://github.com/libnice/libnice.git)
Project: libnice
Toolchain provider: MSYS2

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

If you later need a version tailored specifically for Java bindings (JNI or Panama), that requires a slightly adjusted build configuration.
