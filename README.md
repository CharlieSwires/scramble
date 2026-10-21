# Scramble — Charlie's Arcade

A standalone, **Scramble-inspired side-scrolling game** built on Charles Swires'
Saturn/Galaxian Java framework. Fly through hills and caves, shoot rockets and
flying enemies, bomb ground installations, and destroy fuel tanks to stay airborne.

This is a separate **Scramble** project. It does not modify Galaxian, Saturn or
their saved scores. It is an independent inspired game, not an arcade emulator
or an exact reproduction of the original levels, graphics or scoring.

## Quick start on Windows

1. Extract **Scramble.zip** into a folder of your choice.
2. Open the extracted **Scramble** folder.
3. Double-click **run.bat**.
4. Press **Enter** to start your mission.

You need **Java 8 or later** installed and available on PATH. The ZIP includes a
ready-to-run **Scramble.jar**, so rebuilding is optional.

[OpenJDK downloads](https://jdk.java.net/)

From Command Prompt:

```bat
cd path\to\Scramble
run.bat
```

From Git Bash, Linux or macOS:

```bash
cd path/to/Scramble
./run.sh
```

Use `bash run.sh` if executable permissions were not retained when extracting.
The launch scripts select the project folder as the working directory.
You can also run directly:

```bash
java -jar Scramble.jar
```

## Controls

| Key | Action |
| --- | --- |
| Enter | Start a mission from the title or score table |
| Arrow keys / W A S D | Move up, left, down and right |
| Space | Fire forward; hold for repeated shots |
| B or X | Drop bombs; hold for repeated drops |
| P / Escape during play | Pause or resume |
| M | Mute/unmute effects |
| T | Play a test sound; retry unavailable audio |
| H | Show high scores from title, or return to title |
| Escape on the score table | Return to title |

You can move, fire and bomb together. Diagonal movement is normalised so it does
not move faster than movement along one axis. A maximum of four forward shots
and three bombs can be on screen at once.

Switching to another window pauses the game and releases held controls. Return
and press P or Escape to resume. Keyboard bindings are window-wide. Name entry
stays inside the game window rather than opening a separate modal dialog.

## Your mission

The scenery scrolls continuously from right to left. Keep the ship inside the
open corridor and avoid terrain, rockets, drones and enemy fire.

The route repeats through four sections:

| Section | Features |
| --- | --- |
| Ridgeline | Open sky, uneven ground, fuel tanks, rockets and drones |
| Missile Valley | Open terrain with launching missiles and flying enemies |
| The Caverns | A continuous ceiling and floor forming a tighter flight corridor |
| Fortress Run | Ground turrets and fortified bases, plus fuel tanks and drones |

Each section spans 4,000 world pixels. After all four, the next level increases
scroll speed and enemy pressure. The HUD shows section progress, fuel, lives,
score and best score. Clearing a section awards **500 points**.

### Weapons

**Forward shots** travel horizontally ahead of your ship. They can destroy
flying enemies, launched rockets and ground targets if you fly low enough.

**Bombs** retain forward momentum and accelerate downwards under gravity. Lead
your target: release a bomb before you reach a fuel tank or ground installation.
Bombs and bullets are consumed by terrain or their first target hit.

### Fuel and lives

Fuel drains throughout active flight. Destroy a **FUEL** tank to restore **28%**,
up to 100%. The HUD warns you below 25%. If fuel runs out, you lose a life.

You start with **three lives** and earn another at every **10,000 points**.
A surviving ship relaunches at a safe point in the local corridor after a short
pause, with a full tank and brief protection from enemies. Terrain stays solid;
a protected ship that overlaps it is repositioned into the safe corridor.

After your final life, a qualifying score can be entered in the top-ten table.

### Scores

| Target | Points |
| --- | ---: |
| Rocket on the ground | 50 |
| Launched rocket | 100 |
| Ground turret | 100 |
| Flying drone | 120 |
| Fuel tank | 150, plus fuel |
| Fortress base | 1,000 |
| Section completion | 500 |

## High scores

Scores are saved to **scramble-scores.bin** in the working directory. A new file
is created on your first saved score. Galaxian's and Saturn's score files are not
read or overwritten.

The best ten are shown highest first. A score qualifies when there is room, or
when it beats the lowest score in a full table. New ties appear below older equal
scores. Enter your name and press Enter or click **SAVE SCORE**. Blank names become
**PLAYER**. Names are limited to 20 printable ASCII characters.

Saving a name cannot accidentally start a new game on that same Enter release.
Press Enter again from the table to begin a fresh mission.

The retained score-file format uses the Saturn XOR sequence (seed 73890) and
`ScoreName` rows. This is obfuscation, not secure encryption. If an existing file
cannot be parsed, it is retained and saving is disabled for that session. Back up
or rename it before starting again with a fresh table.

## Graphics, explosions and sound

All scenery, ships, targets and projectiles are drawn directly in Java using
`Graphics2D`. **No external sprite downloads, image libraries or game assets are
required.** Text is drawn at the display resolution and Swing double buffering
combines the scenery and UI into complete frames.

Explosions use **your Saturn algorithm**: 200 particles with random bearing and
speed from 0 to 5, `dx = speed * sin(bearing)` and `dy = speed * cos(bearing)`.
Particles are 3 × 3 pixels and fade from white to black over approximately **5.1
seconds**, preserving the later Galaxian fade-speed change. Motion uses the
equivalent of Saturn's normal 25 updates per second.

The Java audio engine is retained from Galaxian. It synthesizes effects through
`javax.sound.sampled.SourceDataLine`, trying the original Saturn mono format first
and then common alternatives. Press **T** to test sound. Device-opening results,
sound requests and PCM write counters appear in **sound-diagnostics.txt**.

If it is silent despite a READY status, check the Windows audio service, selected
output device and Java's volume in the system mixer. The audio thread cannot
confirm whether speakers or headphones are actually audible.

## Build from source

Install an OpenJDK **JDK**, with `javac` and `jar` available on PATH. There is no
Maven/Gradle requirement and no third-party runtime dependency.

Windows:

```bat
build.bat
run.bat
```

Git Bash / Linux / macOS:

```bash
./build.sh
./run.sh
```

The scripts clear stale class files before compilation. For a manual build with
JDK 9 or later, checking against the Java 8 API:

```bash
javac --release 8 -encoding UTF-8 -d bin src/*.java
jar cfe Scramble.jar Scramble -C bin .
java -jar Scramble.jar
```

With JDK 8, replace `--release 8` with `-source 8 -target 8`. The supplied JAR was
built with `--release 8`. The scripts use source/target options for compatibility
with both older and modern JDKs.

## Eclipse

Use **File → Import → General → Existing Projects into Workspace**, select the
extracted Scramble folder, and finish. `.project` and `.classpath` are included.
Configure a JDK 8 or later, then run **Scramble** as a **Java Application**.

Set the working directory to the Scramble project folder so scores and audio
diagnostics are written there. This is a classpath project without
`module-info.java`.

## Tests and previews

```bat
test.bat
```

or:

```bash
./test.sh
```

The dependency-free headless tests cover:

- Continuous floor/ceiling terrain and safe corridor sizes.
- Four-direction flight, forward bullets and bomb gravity.
- Fuel restoration, exhaustion, crashes, protection and safe respawn.
- Hits on enemies, enemy projectile collisions and scoring.
- Section progression, difficulty and bounded entity lists in a long simulation.
- High-score persistence, in-window submission and working controls after restart.
- Actual Swing renders of title, flight, cave, name-entry and score screens.
- Audio synthesis, PCM encoding and non-silent writes through the real worker
  using a simulated audio device.

Tests passed in the build environment. Sound and interactive Windows play need
checking on your laptop. Preview images are included in **docs/**; regenerated
renders are written under **build/** by the test command.

## Javadoc

```bash
javadoc -encoding UTF-8 -docencoding UTF-8 -charset UTF-8 -private -d javadoc src/*.java
```

Open `javadoc/index.html`. Warnings about missing comments normally do not prevent
generation.

## Project files

```text
Scramble/
  Scramble.jar
  README.md
  CREDITS.md
  build.bat / build.sh
  run.bat / run.sh
  test.bat / test.sh
  src/
    Scramble.java
    FlightPanel.java
    ScrambleModel.java
    Terrain.java
    SaturnExplosion.java
    SoundEffects.java
    HighScores.java
  tests/
  docs/
  bin/
```

## Rights

Copyright © 2026 Charlie's Tenacity Ltd. All rights reserved for project code.
This independent Scramble-inspired game contains no original Konami ROMs,
extracted arcade graphics or recordings. See **CREDITS.md** for the code lineage.
