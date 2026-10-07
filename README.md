# AIPlayer

Real Minecraft client bot plugin for Paper 1.21.11 / Java 25.

This repository is the source of the AIPlayer plugin for the NeverLand Kingdom server.

## Build
GitHub Actions builds the shaded JAR with Java 25:
`mvn -B -U clean package`

## Server
- Paper 1.21.11
- Java 25
- Java + Bedrock through Geyser/Floodgate

## Usage
`/aiplayer start` toggles the AI player.

The bot connects as a real protocol client, observes server GUI/container packets, uses an allowlist for commands, and uses the Bukkit server world for authoritative observations.
