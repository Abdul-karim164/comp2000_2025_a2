import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.swing.Timer;

public class Stage {
    // Grid + actors
    Grid grid;
    List<Actor> actors = new ArrayList<>();

    // Items
    private final EntityBag<Item> items = new EntityBag<>();

    // Player + enemy
    private Rabbit player;
    private Cat enemy;
    private SimpleGameState state = SimpleGameState.Ready;
    private static final int TARGET_SCORE = 5;
    private int score = 0;
    private int timeLeft = 60;

    // Countdown timer
    private javax.swing.Timer tick;
    
    // To move the cat in a random pattern. Exposed so that weather strategies can sample randomness.
    public final java.util.Random rng = new java.util.Random();
    private Cell enemyPrev = null;

    private WeatherManager weatherManager;
    private Supplier<Integer> enemyStepStrategy;
    private BiFunction<List<Cell>, List<Cell>, Cell> enemyChoiceStrategy;
    public List<Actor> listOfPlayers = actors;
    public List<Cell> cellOverlay = new ArrayList<>();
    public java.util.Optional<Actor> playerInAction = java.util.Optional.empty();
    public GameState currentState = new ChoosingActor();

    public Stage() {
        grid = new Grid();
        // Actors
        player = new Rabbit(grid.cellAtColRow(1, 18).get());
        enemy  = new Cat   (grid.cellAtColRow(10, 2).get(), false);
        actors.add(player);
        actors.add(enemy);

        // Items
        spawnCarrots(3);

        // Decrement time while RUNNING
        tick = new Timer(1000, ev -> {
            if (state != SimpleGameState.RUNNING) return;
            timeLeft--;
            if (timeLeft <= 0) {
                state = SimpleGameState.TIMEUP;
                tick.stop();
            }
        });

        // Default enemy movement strategies
        enemyStepStrategy = () -> rng.nextDouble() < 0.9 ? 1 : 2;
        enemyChoiceStrategy = (best, filtered) -> {
            List<Cell> pool = filtered.isEmpty() ? best : filtered;
            return pool.get(rng.nextInt(pool.size()));
        };

        weatherManager = new WeatherManager(grid, this);
        WeatherFeed.getInstance().registerListener(weatherManager);
    }


    public void addPlayer(Actor actor) {
        if (actor != null) {
            actors.add(actor);
        }
    }

    public List<Cell> getClearRadius(Cell from, int size) {
        List<Cell> radius = grid.getRadius(from, size);
        // remove any cell that is occupied by an actor
        List<Cell> available = new ArrayList<>();
        outer: for (Cell c : radius) {
            for (Actor a : actors) {
                if (a.loc == c) {
                    continue outer;
                }
            }
            available.add(c);
        }
        return available;
    }

    // Handle key presses
    public void handleKey(int keyCode) {
        if (state == SimpleGameState.Ready) { startGame(); return; }
        if (state != SimpleGameState.RUNNING) { startGame(); return; }

        Direction d = switch (keyCode) {
            case java.awt.event.KeyEvent.VK_UP    -> Direction.UP;
            case java.awt.event.KeyEvent.VK_DOWN  -> Direction.DOWN;
            case java.awt.event.KeyEvent.VK_LEFT  -> Direction.LEFT;
            case java.awt.event.KeyEvent.VK_RIGHT -> Direction.RIGHT;
            default -> null;
        };
        if (d != null) movePlayer(d);
    }

    public void setEnemyStepStrategy(Supplier<Integer> supplier) {
        if (supplier != null) {
            this.enemyStepStrategy = supplier;
        }
    }

    public void setEnemyChoiceStrategy(BiFunction<List<Cell>, List<Cell>, Cell> fn) {
        if (fn != null) {
            this.enemyChoiceStrategy = fn;
        }
    }

    private void startGame() {
        state = SimpleGameState.RUNNING;
        score = 0;
        timeLeft = 60;

        items.clear();
        spawnCarrots(3);

        player.moveTo(grid.cellAtColRow(1, 18).get());
        enemy .moveTo(grid.cellAtColRow(10, 2).get());

        tick.start();
    }

    private void movePlayer(Direction d) {
        int dc = (d == Direction.LEFT ? -1 : d == Direction.RIGHT ? 1 : 0);
        int dr = (d == Direction.UP   ? -1 : d == Direction.DOWN  ? 1 : 0);

        Cell dest = grid.neighbor(player.getCell(), dc, dr);
        if (dest == null) return;

        player.moveTo(dest);

        // Collect carrot if present
        items.at(dest).ifPresent(it -> {
            if (it.canBeCollectedBy(player)) {
                items.remove(it);
                score++;
                if (score < TARGET_SCORE && items.size() < 3) {
                    spawnCarrots(1);
                }
            }
        });

        // Enemy chases
        chaseEnemy();

        // End checks
        if (enemy.getCell() == player.getCell()) {
            state = SimpleGameState.LOST;
            tick.stop();
        }
        if (score >= TARGET_SCORE) {
            state = SimpleGameState.WON;
            tick.stop();
        }
    }

    private void chaseEnemy() {
        // Determine number of steps using the current strategy
        int steps = enemyStepStrategy.get();
        for (int s = 0; s < steps; s++) {
            // Collect all valid neighbouring cells
            int[][] dirs = { {1,0}, {-1,0}, {0,1}, {0,-1} };
            List<Cell> options = new ArrayList<>();
            for (int[] d : dirs) {
                Cell n = grid.neighbor(enemy.getCell(), d[0], d[1]);
                if (n != null) options.add(n);
            }
            if (options.isEmpty()) return;

            // Find the set of cells that minimise Manhattan distance
            int bestDist = Integer.MAX_VALUE;
            List<Cell> best = new ArrayList<>();
            for (Cell n : options) {
                int dist = Math.abs(n.col - player.getCell().col) + Math.abs(n.row - player.getCell().row);
                if (dist < bestDist) {
                    bestDist = dist;
                    best.clear();
                    best.add(n);
                } else if (dist == bestDist) {
                    best.add(n);
                }
            }
            // Avoid immediate backtracking
            List<Cell> filtered = new ArrayList<>();
            for (Cell n : best) {
                if (enemyPrev == null || n != enemyPrev) filtered.add(n);
            }
            // Delegate the choice to the current choice strategy
            Cell next = enemyChoiceStrategy.apply(best, filtered);
            // Move, update previous position and detect collision
            enemyPrev = enemy.getCell();
            enemy.moveTo(next);
            if (enemy.getCell() == player.getCell()) return;
        }
    }
  


    private void spawnCarrots(int n) {
        java.util.Set<Cell> blocked = new java.util.HashSet<>();
        blocked.add(player.getCell());
        blocked.add(enemy.getCell());
        for (Item it : items.asList()) blocked.add(it.getCell());

        for (int i = 0; i < n; i++) {
            items.add(new Carrot(grid.randomEmptyCell(blocked), grid));
        }
    }

    public Cell getEnemyCell() {
        return enemy.getCell();
    }

    public Cell getPlayerCell() {
        return player.getCell();
    }

    public void paint(Graphics g, Point mouseLoc) {
        grid.paint(g, mouseLoc);

        // Overlay weather effects before drawing items and actors.
        if (weatherManager != null) {
            weatherManager.paint(g);
        }

        // Items
        for (Item i : items.asList()) {
            i.paint(g);
        }

        // Actors
        for (Actor a : actors) {
            a.paint(g);
        }

        // HUD
        g.setColor(Color.BLACK);
        g.drawString("Carrots: " + score + "/" + TARGET_SCORE, 10, 16);
        g.drawString("Time: " + timeLeft + "s", 10, 32);
        g.drawString("State: " + state, 10, 48);
        if (state == SimpleGameState.Ready) {
            g.drawString("Press arrows to start", 10, 64);
        } else if (state == SimpleGameState.WON) {
            g.drawString("You WON! Press arrows to restart.", 10, 64);
        } else if (state == SimpleGameState.LOST) {
            g.drawString("Caught by the cat! Press arrows to restart.", 10, 64);
        } else if (state == SimpleGameState.TIMEUP) {
            g.drawString("Time up! Press arrows to restart.", 10, 64);
        }
    }
}