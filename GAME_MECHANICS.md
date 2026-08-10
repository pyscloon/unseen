# UNSEEN - Game Mechanics

## Game Overview

UNSEEN is a 2D turn-based stealth horror roguelike game. The player explores a hidden dungeon beneath Room 205 of the Engineering Building and must survive multiple floors filled with enemies, traps, darkness, and dangerous terrain. The main goal is to reach the exit tile on each floor without being caught by enemies.

The game focuses on careful movement, limited visibility, sound-based enemy behavior, item usage, and survival strategy. Every action matters because enemies move after the player takes a turn.

## Objective

The objective of the game is to guide the hero safely to the exit tile on each floor.

To win or progress:

1. Move through the dungeon grid.
2. Avoid being seen or reached by enemies.
3. Use items to distract, escape, block vision, or defeat enemies.
4. Reach the exit tile to proceed to the next floor.
5. Survive as many floors as possible.

The player loses if an enemy reaches the same tile as the player.

## Story

Beneath Room 205 in the Engineering Building, under an old software engineering classroom, a hidden dungeon was discovered. A spreading virus twisted the depths, causing monsters to appear in the dark.

The first heroes, Lon, Ron, and Dom, challenged the dungeon before the player. They left behind ancient relics that now appear as usable items throughout the dungeon. The player takes on the challenge and must succeed where the earlier heroes failed.

## How to Play

UNSEEN is played on a 25 by 25 tile grid. The player controls the hero one move at a time. After the player moves, waits, or uses an item, enemies take their turns.

The player must study enemy positions, avoid detection zones, use the environment wisely, and pick the right item for each situation.

Basic rules:

1. The player moves one tile at a time.
2. Every movement or item use counts as a turn.
3. Enemies react after the player acts.
4. Enemies can chase, patrol, detect sound, or guard areas.
5. If an enemy catches the player, the run ends.
6. The player can pick up items found on the map.
7. The player must reach the exit tile to clear the floor.

## Controls

| Key | Action |
| --- | --- |
| W / A / S / D | Move up, left, down, or right |
| Arrow Keys | Alternative movement controls |
| E | Pick up item or interact with objects |
| Space | Wait or skip a turn |
| 1 | Use Noise Maker |
| 2 | Use Smoke Bomb |
| 3 | Use Flare / Lantern |
| 4 | Use Shuriken |
| 5 | Use Grappling Hook |
| 6 | Use Holy Cross in Horror Mode |
| Enter / Space | Confirm selected target |
| ESC | Cancel targeting, pause, or return to menu |
| P | Pause or resume |
| M | Toggle music |
| N | Toggle sound effects |
| R | Restart after losing |
| H | Open tutorial from the main menu |
| A | Open achievements from the main menu |
| X | Toggle Horror Mode from the main menu |
| V | Toggle quest HUD while playing |

## Player Mechanics

The player controls one hero who moves through the dungeon. The hero can:

1. Move one tile per turn.
2. Pick up items from the floor.
3. Use items from the inventory.
4. Wait for one turn.
5. Hide or interact with certain objects such as barrels.
6. Escape traps by attempting movement.
7. Reach the exit tile to proceed.

The player must avoid direct contact with enemies. Stealth is more important than fighting because enemies are dangerous and items are limited.

## Turn-Based System

UNSEEN uses a turn-based system. A turn happens whenever the player moves, waits, or uses an item. After the player's action, enemies update their behavior.

Enemy reactions may include:

1. Continuing a patrol path.
2. Investigating a sound.
3. Chasing the player after detection.
4. Searching after losing sight of the player.
5. Placing traps on higher floors.
6. Moving toward the player in Horror Mode.

This system makes the game strategic because rushing can quickly lead to being detected.

## Enemy Mechanics

### Patrol Guard

The Patrol Guard follows a fixed route. If it sees the player, it will chase aggressively. If it loses sight of the player, it searches briefly before returning to its patrol.

### Hunter

The Hunter uses smarter pathfinding and actively hunts the player. On higher floors, it may place Sticky Traps in the player's path. Once alerted, it becomes persistent and dangerous.

### Sentry

The Sentry is a stationary guard with a wide detection arc. When it spots the player, it can alert nearby enemies. It may also leave its post and chase the player.

### Crawler

The Crawler is blind and does not depend on light or line-of-sight. It detects the player through proximity or sound. If triggered, it charges faster than normal enemies. Smoke bombs are not effective against it, but Noise Makers can lure it away and Shurikens can eliminate it.

### Stalker

The Stalker appears in Horror Mode if the player takes too long on a floor. It is a persistent and invincible predator. The player cannot kill it and must escape quickly.

## Item Mechanics

### Noise Maker

The Noise Maker can be thrown to a target tile. Enemies within range investigate the sound, allowing the player to lure them away from important paths.

### Smoke Bomb

The Smoke Bomb activates instantly around the player. It creates a smoke cloud that blocks enemy line-of-sight for several turns. It is useful for emergency escapes.

### Flare / Lantern

The Flare or Lantern can be thrown to a target tile to illuminate an area. It helps the player scout ahead and manage dark areas.

### Shuriken

The Shuriken is a ranged weapon. The player aims using movement directions and throws it in a straight line. It can silently eliminate the first enemy it hits, but it is stopped by walls.

### Grappling Hook

The Grappling Hook lets the player target a wall within range and zip to the nearest floor tile beside it. It is useful for escaping enemies or crossing dangerous spaces quickly.

### Holy Cross

The Holy Cross only works in Horror Mode. It purifies the floor, removes certain horror effects, and can banish Horror Mode threats. It is rare and should be saved for dangerous situations.

## Environment Mechanics

### Walls

Walls block movement and line-of-sight. They can be used to hide from enemies or break a chase.

### Exit Tile

The exit tile allows the player to clear the current floor and move to the next one.

### Puddles

Puddles are hazards. Stepping on one creates a loud splash that alerts nearby enemies.

### Sticky Traps

Sticky Traps can trap the player temporarily. The player must attempt movement to struggle free, which costs turns and gives enemies time to move.

### Barrels

Barrels can be interacted with and may help the player hide or avoid enemy attention.

### Campfire

A campfire can act as a sanctuary. Resting near it can restore health, but its healing use is limited.

## Horror Mode

Horror Mode is an optional mode that makes the game more intense and dangerous. It can be toggled from the main menu using the X key.

Horror Mode adds:

1. Total darkness.
2. Flickering or unreliable light tools.
3. More intense sound effects.
4. Psychological horror events.
5. Stronger pressure to finish each floor quickly.
6. Special threats such as the Stalker.
7. The Holy Cross item as a purification tool.

Horror Mode is designed for players who want a harder and scarier version of the game.

## Win and Lose Conditions

The player clears a floor by reaching the exit tile.

The player loses if:

1. An enemy steps onto the player's tile.
2. The player is caught while trapped or cornered.
3. The player fails to escape dangerous Horror Mode threats.

After losing, the player can restart the game.

## Scoring and Progression

The game uses floor progression. Each cleared floor increases the challenge by introducing more threats, stronger enemy behavior, or harder layouts. The player may also receive rewards or choices between floors.

Possible progression elements include:

1. Higher enemy count.
2. More complex enemy behavior.
3. Additional traps.
4. More dangerous Horror Mode events.
5. Reward choices after clearing floors.

## Electronics Used to Play

UNSEEN is a computer-based game. The electronics and software used to play include:

1. Laptop or desktop computer.
2. Keyboard for movement, item use, menu navigation, and gameplay controls.
3. Monitor or laptop screen for displaying the game.
4. Speakers or headphones for music, sound effects, alerts, and horror audio.
5. Java Runtime Environment to run the game.
6. IntelliJ IDEA or another Java-supported IDE for compiling and running during development.
7. Optional mouse or touchpad for menu interaction and selecting targets, depending on the build.

## Software and Technical Components

The game was developed using Java. It uses Java Swing for the game window and rendering.

Main technical components:

1. Java programming language.
2. Java Swing JFrame and JPanel for the game window.
3. Tile-based map system.
4. Player and enemy entity classes.
5. A* pathfinding for enemy movement.
6. Line-of-sight detection.
7. Turn manager for player and enemy actions.
8. Sound manager for music and sound effects.
9. Asset loader for sprites and audio.
10. Keyboard input handler for controls.

## Materials and Game Assets

The game includes visual and audio assets such as:

1. Hero sprite.
2. Enemy sprites.
3. Wall and floor tiles.
4. Item icons.
5. Exit tile.
6. Horror images and effects.
7. Footstep sounds.
8. Alert sounds.
9. Item sound effects.
10. Horror music and jumpscare audio.

## Summary

UNSEEN is a stealth-based survival game where the player must think carefully before each move. The combination of turn-based movement, enemy detection, limited items, sound hazards, and Horror Mode creates a tense dungeon escape experience. The main challenge is not only reaching the exit, but doing so without being seen, trapped, or hunted down.
