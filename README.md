# Welcome to COMP2000 - Object Oriented Programming Practices
## Session 2, 2025

Please ensure that you follow the weekly updates in this repository

You are free to clone this repository into your own hosted git environment, such as Github, Bitbucket, or Gitlab.

*However*, please be aware that any repository containing your assignment code **must** be made private. Any repository with assignment code that is public available, or found to be shared with other students, will be considered a violation of the academic integrity policy.



# COMP2000 Assignment 2 – Weather‑Aware Grid Game
## Overview

This assignment builds on the week 11 grid game by integrating a live weather feed and refactoring the code base to use well known object‑oriented design patterns, lambdas and the Java Stream API. The game starts from the simple “Carrot Dash” developed in assignment 1: the player controls a rabbit on a 20×20 grid, collects carrots and avoids a cat. In assignment 2 the world has become dynamic; a remote server at 13.238.167.130/weather streams rainfall, temperature and wind data for arbitrary world coordinates. Every few seconds the game receives a line such as:

1760664486 rain 8 -7 0.38
1760664486 windx 8 -7 0.50
1760664486 windy 8 -7 0.54
1760664486 temp 8 -7 0.54

Each record contains a timestamp, an attribute name, a world‑space (x,y) and a value between 0.0 and 1.0. During play the grid becomes overlaid with semi‑transparent colours and arrows indicating local rain, heat and wind. The enemy’s behaviour adapts to the weather: heavy rain slows the cat to one step per turn, heat can make it skip turns entirely, and strong winds cause it to lose its sense of direction.

Play the game using the arrow keys to start and move the rabbit. Carrots are orange squares. Collect five carrots before time runs out. The cat will chase you, and its speed and direction will change depending on the weather. You will observe how the colours and arrows overlaid on the grid reflect the current conditions from the server. To reset the game after winning, losing or timing out, simply press any arrow key.

## New Functionality

- Live weather overlay : A background thread connects to the server and reads a continuous stream of weather events. Each event is mapped to a grid cell and stored. When drawing the grid the game decorates each cell with zero or more WeatherEffects.

- RainEffect: Paints a blue tint whose opacity is proportional to the rainfall value. Light rain barely colours the cell whereas heavy rain turns it deep blue.

- HeatEffect: Paints a red tint for high temperatures (values > 0.5).

- WindEffect: Draws a pale aqua background and a cyan arrow pointing in the direction of the wind. The arrow’s length and thickness scale with the wind magnitude.

- Adaptive enemy behaviour: The cat (enemy) no longer chases the rabbit blindly. The WeatherManager monitors the current cell occupied by the cat and adjusts two strategies at runtime:

Step strategy (via a Supplier<Integer>): determines how many cells the cat will move this turn. In normal weather it occasionally takes two steps. If the local rainfall ≥ 0.2, it is limited to one step, and if the local temperature ≥ 0.5 there is a 50 % chance it will not move at all.

Choice strategy (via a BiFunction<List<Cell>, List<Cell>, Cell>): chooses the next cell from the set of neighbours that minimise the Manhattan distance to the rabbit. Under strong winds (wind magnitude ≥ 0.3) the cat picks randomly from all best options, simulating disorientation.

# Design Patterns Used

The solution deliberately employs several design patterns to keep the code base flexible and maintainable. I have documented each pattern here along with its purpose and benefits.

## Observer Pattern

The weather feed is implemented as a subject and the WeatherManager is an observer. WeatherFeed maintains a list of listeners and notifies them whenever a new line arrives from the server. This decouples the networking layer from the game logic (the stage has no knowledge of HTTP or threads). It simply registers the manager and receives callbacks. The observer pattern simplifies testing and makes it easy to reuse the feed in other games or modules.

## Decorator Pattern

Each cell on the grid can have multiple simultaneous weather effects. Rather than subclassing Cell for every combination, a lightweight WeatherEffect interface with a single paint(Graphics, Cell) method is introduced. Concrete decorators (RainEffect, HeatEffect, WindEffect) implement this interface and draw semi‑transparent overlays on top of the cell without altering its state. The WeatherManager composes a list of decorators for each affected cell and iterates through them when painting. This pattern allows new effects to be added in isolation and keeps the core Cell class untouched.

## Strategy Pattern

The cat’s movement behaviour is encapsulated in two strategy objects: a step supplier and a choice function. In the stage’s constructor these strategies are initialised to default lambdas (mostly one or two steps, and a simple “best neighbour avoiding backtracking” policy). When WeatherManager processes a new event it calls updateStrategies() which may replace these lambdas based on the local rain, temperature and wind intensity. This pattern allows the high‑level game logic (Stage.chaseEnemy) to remain the same while the details of how many steps to take and which neighbour to pick can vary at runtime. The strategies are first‑class objects, so additional behaviours could be added by swapping in new lambdas.

## State Pattern

The code base distinguishes between two notions of “state”. The SimpleGameState enum controls the high‑level flow of the weather game (ready, running, won, lost, time up). Separately, the turn‑based extension uses the GameState interface implemented by classes such as ChoosingActor and BotMoving. The stage holds a reference to the current turn‑based state and delegates mouse clicks and painting to it. This separation of concerns allows the game to switch behaviours without conditionals scattered throughout the code. Adding a new turn‑based phase requires only a new class implementing GameState.

# Lambdas and Streams

- Event streaming: WeatherFeed uses BufferedReader.lines() to obtain a Stream<String> from the HTTP response. The .forEach() terminal operation processes each line without manually managing loops or indexing. Each line is parsed into a WeatherEvent record via a helper and immediately dispatched to all listeners using a lambda.

- Timer and key handling: The countdown timer is created with new Timer(1000, ev -> { … }). The lambda removes the need for an anonymous ActionListener class. Similarly, the key listener is implemented with a single lambda overriding keyPressed to call handleKey().

- Movement strategies: The enemy’s movement strategies are provided as lambdas implementing Supplier<Integer> and BiFunction<List<Cell>, List<Cell>, Cell>. Swapping strategies is as simple as assigning a new lambda, which satisfies the Strategy pattern’s interface.

- Stream operations on collections: In the turn‑based extension, getClearRadius() uses a Java Set and filters out any cell occupied by a player. The method returns a List<Cell> collected from the filtered results. Although the weather manager now bases its decisions on local weather, the initial implementation computed global maxima via weatherByCell.values().stream().mapToDouble(…).max().

# Interpreting Server Values

The weather server reports four attributes in the range 0.0–1.0:

## Attribute: Rain (rain)
 Interpretation in game: Amount of precipitation
 Effect on gameplay: A value of 0.0 means dry, 0.2 is a drizzle and values ≥ 0.8 indicate heavy rain. Cells with rain > 0.2 are tinted blue. When the cat stands on such a cell it moves at most one step per turn (as if trudging through mud).

## Attribute: Temperature (temp)
Interpretation in game: Relative heat
Effect on gameplay: 0.0 represents cold, 0.5 is mild, and values approaching 1.0 represent extreme heat.	Cells with temperature > 0.5 are tinted red. If the cat is on such a cell there is a 50 % chance it will not move at all that turn, modelling heat exhaustion.

## Attribute: Wind X (windx) and Wind Y (windy)
Interpretation in game: Normalised wind vector components
Effect on gameplay: 0.5 means no wind, values below 0.5 blow towards the negative axis and values above 0.5 blow towards the positive axis.	The wind vector is mapped into the range [−1, 1] to compute a direction and magnitude. Cells with wind magnitude ≥ 0.02 are tinted pale aqua and display a cyan arrow. When the magnitude ≥ 0.3 the cat uses a random choice strategy to pick its next move, reflecting the disorienting effect of strong wind.

The origin of the world coordinate system (0,0) is at the centre of the grid. Weather events falling outside the 20×20 board are ignored. For each cell the latest values of rain, temperature and wind are stored and updated whenever a new record arrives, and the old values are overwritten so stale weather gradually disappears as new data arrives.