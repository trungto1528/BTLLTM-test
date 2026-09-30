# Jump King Multiplayer

A real-time multiplayer platform game built with Java 21, JavaFX, Spring Boot WebSocket, and Maven. The gameplay is inspired by *Jump King*: players control a character that climbs a vertical map by charging and releasing jumps while controlling horizontal movement.

The project follows a client-server architecture. The server owns the authoritative game simulation, while each JavaFX client handles rendering, input, local prediction, reconciliation, and interpolation of remote players.

## Features

- Real-time multiplayer over WebSocket.
- Room creation, room discovery, joining, leaving, and host management.
- Host-controlled game start.
- Vertical platforming map with multiple floors.
- Directional movement with keyboard input.
- Charge-based jumping.
- Server-authoritative physics.
- Fixed 40 TPS game simulation.
- Periodic authoritative world snapshots.
- Client-side prediction for responsive local movement.
- Server reconciliation using input sequence numbers.
- Interpolation for remote players.
- Handling of player departure and host changes.
- JavaFX client interface for menu, room, lobby, and game screens.

## Technology Stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Client UI | JavaFX 21.0.5 |
| Networking | WebSocket |
| Server | Spring Boot WebSocket |
| Build | Maven |
| Architecture | Client-Server |
| Simulation | Fixed timestep, 40 TPS |

## Project Structure

```text
BTLLTM-test/
├── client/
│   ├── pom.xml
│   └── src/main/java/com/test/
│       ├── GameApp.java
│       ├── Main.java
│       ├── core/
│       │   ├── ClientPlayerController.java
│       │   ├── GameScene.java
│       │   ├── GameWebSocketClient.java
│       │   ├── Platform.java
│       │   └── Player.java
│       └── ui/
│           ├── CreateRoomView.java
│           ├── FindRoomView.java
│           ├── JoinRoomView.java
│           ├── LobbyView.java
│           └── MainMenuView.java
│
├── common/
│   └── src/main/java/com/test/common/
│       ├── GameConfig.java
│       ├── InputCommand.java
│       ├── PhysicsEngine.java
│       ├── PlatformData.java
│       └── PlayerState.java
│
├── server/
│   ├── pom.xml
│   └── src/main/java/com/test/
│       ├── GameServer.java
│       ├── GameWebSocketHandler.java
│       ├── GameRoom.java
│       ├── PlayerSession.java
│       └── RoomManager.java
│
└── pom.xml
```

## Architecture

The project is divided into three Maven modules:

### Client

The client is a JavaFX application.

Its responsibilities are:

- Display the game and user interface.
- Capture keyboard input.
- Send input commands to the server.
- Predict local player movement.
- Reconcile local state with authoritative server state.
- Interpolate remote players.
- Render the map, platforms, and players.

### Server

The server is a Spring Boot WebSocket application.

Its responsibilities are:

- Accept WebSocket connections.
- Assign player sessions.
- Manage rooms.
- Manage hosts.
- Validate and queue player input.
- Run authoritative physics.
- Maintain room-specific game ticks.
- Broadcast world snapshots.
- Notify clients when players leave.

### Common

The common module contains data structures and game logic shared between client and server, including:

- `GameConfig`
- `InputCommand`
- `PhysicsEngine`
- `PlatformData`
- `PlayerState`

## Runtime Flow

A typical game session follows this flow:

```text
Start Client
    |
    v
Connect WebSocket
    |
    v
Receive WELCOME
    |
    v
Create / Find / Join Room
    |
    v
Lobby
    |
    v
Host starts game
    |
    v
Server sends GAME_STARTED
    |
    v
GameScene
    |
    +---- Player Input ----> Server
    |                         |
    |                         v
    |                   Input Queue
    |                         |
    |                         v
    |                    Game Tick
    |                         |
    |                         v
    |                      Physics
    |                         |
    |                         v
    +<----- WORLD_STATE ------+
```

## Controls

| Key | Action |
|---|---|
| `A` / `Left Arrow` | Move left |
| `D` / `Right Arrow` | Move right |
| `Space` | Charge/release jump |

The current source does **not** implement the planned in-game ESC pause yet.

## Game Simulation



### Fixed Timestep

The server runs one global game loop at 40 ticks per second.

```text
TICK_RATE = 40 TPS
TICK_DT   ≈ 25 ms
```

A fixed timestep prevents the physics result from depending directly on the frame rate of the machine running the server.

For each started room, the server performs:

1. Increment the room tick.
2. Process queued player inputs.
3. Update jump charging.
4. Run physics for every player.
5. Periodically broadcast an authoritative snapshot.

The server uses an absolute deadline for its loop rather than simply sleeping for a fixed duration after each tick, which helps reduce timing drift.

### Snapshot Rate

Simulation runs at 40 TPS while world snapshots are sent less frequently.

With the current configuration:

```text
40 simulation ticks / second
20 world snapshots / second
1 snapshot every 2 simulation ticks
```

This reduces network traffic while retaining a high-frequency authoritative simulation.

## Client Prediction

Waiting for a server response before moving the local player would make controls feel delayed.

Instead, the client applies local input immediately:

```text
Keyboard Input
      |
      +------> Local Prediction
      |
      +------> WebSocket INPUT
                    |
                    v
                  Server
```

Each input receives an increasing sequence number:

```text
INPUT 1
INPUT 2
INPUT 3
...
```

The server records the last input sequence it has processed.

## Server Reconciliation

The server is authoritative over the actual game state.

When a world snapshot arrives, the local client compares the server's acknowledged input sequence with its own pending inputs.

For example:

```text
Server processed: 103

Client pending:
104
105
106
```

The client:

1. Restores the authoritative server state.
2. Removes acknowledged inputs.
3. Replays remaining unacknowledged inputs.
4. Continues local prediction.

This reduces divergence between the client simulation and the server simulation while keeping local controls responsive.

## Remote Player Interpolation

Remote players are rendered using interpolation rather than jumping directly between network snapshots.

Conceptually:

```text
Previous Snapshot
       |
       v
   Interpolation
       |
       v
Current Snapshot
```

If a remote player is at `x = 100` in one snapshot and `x = 110` in the next, the client renders intermediate positions between those snapshots.

This reduces visible jitter caused by the difference between the network snapshot rate and the display frame rate.

## Network Protocol

The client and server communicate using text-based WebSocket messages.

Important messages include:

### Connection

```text
WELCOME|playerId
```

The server assigns an ID to a newly connected player.

### Room

Examples include:

```text
CREATE_ROOM
JOIN_ROOM|roomId
LEAVE_ROOM
FIND_ROOMS
ROOM_CREATED|roomId
ROOM_JOINED|roomId
ROOM_STATE|...
ROOM_LEFT|roomId
ROOM_ERROR|...
```

### Game Start

The host requests:

```text
START_GAME
```

The server confirms the start with:

```text
GAME_STARTED|roomId
```

The client then creates and displays the game scene.

### Player Input

Input is sent as:

```text
INPUT|sequence|action
```

Current actions include:

```text
LEFT_PRESS
LEFT_RELEASE
RIGHT_PRESS
RIGHT_RELEASE
JUMP_START
JUMP_RELEASE
```

### World State

The server periodically sends:

```text
WORLD_STATE|TICK|serverTick|PLAYER|...
```

Each player snapshot contains authoritative position, velocity, movement, jump, and input-acknowledgement information.

## Room Isolation

Game simulation is performed per room.

Conceptually:

```text
GameServer
│
├── Room A
│   ├── Player A1
│   └── Player A2
│
├── Room B
│   ├── Player B1
│   └── Player B2
│
└── Room C
    └── Player C1
```

A player's world state is broadcast only to players in the same room.

Rooms that have not started do not run gameplay physics.

## Player Disconnect

The server handles both explicit room leaving and WebSocket disconnection.

When a player leaves a room:

1. The player is removed from the room.
2. The server determines whether the player was the host.
3. If the room still contains players, a new host is selected.
4. Remaining clients receive a `PLAYER_LEFT` notification.
5. The room state is broadcast again.

The notification format is:

```text
PLAYER_LEFT|roomId|playerId|wasHost|newHostId
```

This allows clients to update their UI and room state without restarting the game.

## Map

The current map is designed as a vertical climbing course.

Current dimensions:

```text
Map:      800 x 1800
Viewport: 800 x 600
```

The map contains:

- Multiple platform sections.
- Left and right boundary walls.
- A bottom floor.
- Several vertical floors/areas used by the camera system.

The same platform layout is represented on both client and server through the common platform data structures.

## Camera

Because the world is taller than the viewport, the client uses a vertical camera system.

The game world is rendered in a larger coordinate system and translated vertically according to the player's current floor.

This allows the 800x600 viewport to display different sections of the 800x1800 map while keeping the player visible.

## Physics

The server owns the authoritative physics state.

Important player properties include:

- Position `x, y`
- Velocity `velocityX, velocityY`
- Grounded state
- Jump charging state
- Jump direction
- Jump power
- Facing direction
- Horizontal movement state

The client contains corresponding state and physics logic for local prediction.

The server's result remains authoritative when the two simulations differ.

## Building

### Requirements

Install:

- JDK 21
- Maven 3.9+ recommended
- A JavaFX-compatible desktop environment

Verify Java:

```bash
java -version
```

Verify Maven:

```bash
mvn -version
```

### Build the entire project

From the repository root:

```bash
mvn clean package
```

This builds:

- `common`
- `server`
- `client`

### Run the Server

The server is a Spring Boot application.

From the repository root, the server module can be started with:

```bash
mvn -pl server spring-boot:run
```

The server should be running before starting the client.

### Run the Client

From the repository root:

```bash
mvn -pl client javafx:run
```

Start multiple client instances when testing multiplayer behavior.

## Multiplayer Test

A basic local test can use:

```text
                 Server
                   |
        +----------+----------+
        |          |          |
     Client 1   Client 2   Client 3
```

Recommended test sequence:

1. Start the server.
2. Start two or more clients.
3. Connect all clients.
4. Create a room with one client.
5. Join the same room from the other clients.
6. Verify the lobby state.
7. Start the game from the host.
8. Move and jump with different clients.
9. Observe remote player movement.
10. Close one client and verify the remaining clients receive the departure event.

## Synchronization Design Summary

The current synchronization model can be summarized as:

```text
                         SERVER
                           |
                    Authoritative State
                           |
                 +---------+---------+
                 |                   |
             Physics             Snapshots
                 |                   |
                 v                   v
              Player             Clients
                                    |
                    +---------------+---------------+
                    |                               |
              Local Player                    Remote Players
                    |                               |
             Prediction +                    Interpolation
             Reconciliation
```

This separation is important:

- The server decides the actual game state.
- The local client predicts its own movement for responsiveness.
- The server periodically corrects the local client.
- Remote players are interpolated for smooth rendering.

## Main Source Files

### Client

- `GameApp.java` — application lifecycle, screens, room/game transitions, and server messages.
- `GameScene.java` — game rendering, input, local prediction, remote interpolation, camera, and map.
- `GameWebSocketClient.java` — WebSocket connection and message routing.
- `ClientPlayerController.java` — local player simulation.
- `Player.java` — player rendering.
- `Platform.java` — platform rendering.

### Server

- `GameServer.java` — global game loop, room simulation, physics tick, and world snapshots.
- `GameWebSocketHandler.java` — WebSocket message handling and connection lifecycle.
- `PlayerSession.java` — player state and queued input processing.
- `GameRoom.java` — room state and room players.
- `RoomManager.java` — room management.

### Common

- `GameConfig.java` — shared timing and game configuration.
- `InputCommand.java` — input command representation.
- `PhysicsEngine.java` — shared movement/physics logic.
- `PlatformData.java` — platform collision data.
- `PlayerState.java` — player state representation.

## Development Notes

When modifying the networking or simulation code, keep the following principles:

1. The server remains authoritative.
2. Client input sequence numbers must remain monotonic.
3. Reconciliation should only use authoritative server state.
4. Remote players should not be directly snapped every render frame.
5. Room A must never receive gameplay state belonging to Room B.
6. Gameplay simulation should remain fixed-timestep.
7. Network callbacks should not directly manipulate JavaFX nodes from a WebSocket thread; UI changes should be performed on the JavaFX application thread.
8. Changes to the network protocol must be updated consistently on both client and server.

## Project Status

Implemented in the current source:

- WebSocket client/server connection.
- Player ID assignment.
- Room creation, discovery, joining and leaving.
- Four-player room capacity.
- Host management and host transfer.
- Host-controlled game start.
- Server-authoritative physics.
- Fixed **40 TPS** simulation.
- **20 snapshots/second**.
- Input sequencing and server ACK.
- Client-side local prediction.
- Server reconciliation with unacknowledged input replay.
- Remote-player interpolation.
- Player departure notification.
- Vertical camera and platform map.

Not yet implemented in the current source:

- In-game ESC local pause.
- Persistent disconnected-player visual indicator above the last player position.
- Player display names.
- Player-selected colors.
- In-game player height scoreboard/table.

These should not be described as completed features until they are present in the source.

## License


This project is developed as an academic/course project.

Unless another license is added to the repository, the source code should be treated as project-specific coursework rather than as an independently licensed open-source library.
