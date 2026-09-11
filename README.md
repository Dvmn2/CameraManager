# CameraManager

Fabric client mod for Minecraft 1.21.11. Applies camera effects received from the companion CameraManagerPlugin Paper
plugin.

This project is under active development. The only implemented feature at this time is camera shake.

## Requirements

- Fabric Loader
- Fabric API
- Minecraft 1.21.11
- Client-side only; this mod is not required on the server, but a server running CameraManagerPlugin is required to
  trigger effects

## Features

### Camera shake

Applies a temporary, randomized offset to camera rotation and position. The shake fades out over its configured duration
and does not persist across disconnecting from a server.

## How it works

The mod listens for two custom network payloads sent by the server plugin:

- `cameramanager:shake` — starts a shake with the given angle offset, position offset, and duration (in ticks)
- `cameramanager:shake_stop` — clears all active shakes

Camera offsets are applied via a mixin into `Camera#update`, after the vanilla camera position and rotation have been
computed.

## Installation

1. Install Fabric Loader and Fabric API for Minecraft 1.21.11.
2. Place the mod jar in the `mods` folder.
3. Connect to a server running CameraManagerPlugin to receive camera effects.

## Notes

Installing this mod without a server running CameraManagerPlugin has no visible effect, as the mod does not trigger any
effects on its own.
