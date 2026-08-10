# UNSEEN

UNSEEN is a 2D turn-based stealth horror roguelike built in Java. The player explores a hidden dungeon, avoids enemies, uses items, and tries to reach the exit tile on each floor without being caught.

## Requirements

- Java Development Kit (JDK)
- Windows PowerShell, Command Prompt, Git Bash, or an IDE such as IntelliJ IDEA
- Keyboard
- Speakers or headphones recommended for music and sound effects

## How to Launch the Game

### Option 1: PowerShell Build Script

From the project folder, run:

```powershell
.\build.ps1
java -cp out unseen.game.Game
```

The build script cleans the `out` folder, compiles the Java source files, and copies the game assets into the output folder.

### Option 2: Manual PowerShell Commands

From the project folder, run:

```powershell
javac -d out (Get-ChildItem -Recurse -Filter *.java src/unseen | ForEach-Object { $_.FullName })
xcopy /E /I /Y src\unseen\assets out\unseen\assets
java -cp out unseen.game.Game
```

### Option 3: Git Bash or Unix-Like Shell

From the project folder, run:

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
cp -r src/unseen/assets out/unseen/assets
java -cp out unseen.game.Game
```

## How to Play

Reach the exit tile on each floor while avoiding enemies. Every move or item use costs one turn, and enemies move after the player acts.

Basic controls:

| Key | Action |
| --- | --- |
| W / A / S / D | Move |
| Arrow Keys | Move |
| E | Pick up item or interact |
| Space | Wait or confirm |
| 1 | Use Noise Maker |
| 2 | Use Smoke Bomb |
| 3 | Use Flare / Lantern |
| 4 | Use Shuriken |
| 5 | Use Grappling Hook |
| 6 | Use Holy Cross in Horror Mode |
| ESC | Cancel, pause, or return to menu |
| P | Pause or resume |
| M | Toggle music |
| N | Toggle sound effects |
| R | Restart after losing |
| X | Toggle Horror Mode from the main menu |

## Project Structure

```text
src/unseen/ai        Enemy pathfinding and line-of-sight logic
src/unseen/assets    Sprites, images, and sound files
src/unseen/entities  Player and enemy classes
src/unseen/game      Main game state and turn logic
src/unseen/items     Usable item classes
src/unseen/map       Tile map and map generation
src/unseen/ui        Rendering and input handling
src/unseen/utils     Constants, asset loading, and sound management
```

## Main Class

The game starts from:

```text
unseen.game.Game
```

Run command:

```powershell
java -cp out unseen.game.Game
```
