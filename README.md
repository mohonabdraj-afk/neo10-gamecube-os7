# Neo10 GameCube OS7 (Clean LSPosed Module)

100% open, zero-permission LSPosed module tailored for **iQOO Neo 10 CN (OriginOS 7)**.

## Features
- Auto-upgrades Monster mode to **MONSTER+** on game launch (no cooler required).
- Unlocks **144 FPS Frame Interpolation** in Game Tools & Q Chip Zone (`of.j`, `of.h`, `yb.b`).
- Blocks GameCube's internal overheat shutdown for 144 FPS (`h.X0`, `h.V0`, `h.u0`).
- **Zero permissions** (no Internet, no Storage, no Root shell, no Macros). Scope: `com.vivo.gamecube` only.

## 1-Click Build via GitHub Actions
1. Create a new GitHub repository (public or private).
2. Extract this ZIP and push all files (including `.github/workflows/build.yml`) to the repository root.
3. Open the **Actions** tab -> select **Build Neo10 GameCube OS7 APK** -> tap **Run workflow** (or wait for the automatic push build).
4. Once green (~1 min), scroll down to **Artifacts** and download `Neo10_GameCube_OS7_Clean_v1.0_APK`.
5. Unzip and install `Neo10_GameCube_OS7_Clean_v1.0.apk` on your phone, enable it in LSPosed (scope: `com.vivo.gamecube` only), and Force Stop Game Cube.

## Build on Phone (AndroidIDE)
1. Extract this ZIP to a folder on your phone.
2. Open the folder in **AndroidIDE**.
3. Wait for Gradle Sync, then tap the **Play / Build APK** button.
