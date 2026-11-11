<p align="center">
  <img src="full/src/main/resources/assets/shardclient/icon.png" width="96" alt="ShardClient logo">
</p>

<h1 align="center">ShardClient</h1>

<p align="center">
  Fabric client mods for the <a href="https://shardpvp.xyz">ShardPvP</a> practice server · Minecraft 26.2
</p>

---

## Products

ShardClient ships as two separate mods. Install **one** of them; neither is required to play on the server.

|  | **Full Client** | **Light Version** |
|---|---|---|
| Mod id | `shardclient` | `shardclient-light` |
| Practice server integration | ✓ | ✓ |
| Standalone features (module system, ClickGUI, Fullbright) | ✓ | — |
| Works on other servers and in singleplayer | ✓ | stays inactive |

Both are built from the same shared code, so the server integration looks and behaves the same in each.

## Status

Work in progress. What exists today:

- **Server connection.** Detects the ShardPvP server, completes a versioned handshake and receives queue,
  match-found and notification events. Confirmed working against the live server.
- **Interface foundation.** Animation engine, layout system and components (panels, buttons, progress bars,
  scroll areas, item icons, player faces) with three looks: *ShardPvP Dark* (default), *ShardPvP Light* and
  *Minecraft*.

Coming next: the in-game screens (queue, match found, notifications, server menus), then the Full Client's
module system, ClickGUI and Fullbright.

## Install

Requires Minecraft 26.2, Fabric Loader 0.19.3 or newer and Fabric API.

1. Download `shardclient-full-…jar` **or** `shardclient-light-…jar`.
2. Put it in your `mods` folder next to Fabric API.

## Privacy and safety

This source is public so anyone can check what the mods do.

- They talk only to the server you are connected to, over one plugin channel (`shardclient:main`) inside the
  normal Minecraft connection. They open no connections of their own.
- They stay silent unless the server announces that channel: nothing is sent on other servers or in singleplayer.
- The only message a client sends is a hello: which product it is, its version, and the protocol versions and
  features it supports ([`Hello.java`](protocol/src/main/java/net/am7n/shardclient/protocol/packet/Hello.java)).
- They do not read your session token, files, clipboard or anything outside the game, and contain no obfuscated
  or pre-built code. The only binary in the repository is the standard Gradle wrapper.

## Building

Requires JDK 25. Everything else is fetched by the Gradle wrapper (checksum pinned).

```sh
./gradlew :full:build      # full/build/libs/shardclient-full-<version>+mc26.2.jar
./gradlew :light:build     # light/build/libs/shardclient-light-<version>+mc26.2.jar
./gradlew build verifyProductJars
```

Each product builds on its own. `verifyProductJars` fails the build if the Light jar contains Full Client code
or the other way round.

**Verifying a release:** builds are reproducible. Build the same version yourself and compare the SHA-256 of
your jar with the released one.

## Project layout

| Module | Contents |
|---|---|
| `protocol` | The wire format. Plain Java with no Minecraft code; the server plugin uses the same classes. |
| `shared` | Code both products use: networking, server integration, animation, rendering and UI. |
| `full` | The Full Client. |
| `light` | The Light Version. |
| `showcase` | Development only, never released: a gallery screen for checking the UI in game. |

```sh
./gradlew :showcase:runClient            # opens the gallery on the title screen
./gradlew :showcase:runClient -Pcapture  # saves screenshots to showcase/run/screenshots and quits
```

## Credits

- [Inter Tight](https://github.com/rsms/inter-tight) by The Inter Project Authors, SIL Open Font License 1.1
  (`shared/src/main/resources/licenses/inter-tight`).
- Built with [Fabric](https://fabricmc.net).
