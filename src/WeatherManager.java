import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WeatherManager implements WeatherListener {
    private final Grid grid;
    private final Stage stage;
    private final Map<Cell, CellWeather> weatherByCell = new ConcurrentHashMap<>();
    private final Map<Cell, List<WeatherEffect>> effectsByCell = new ConcurrentHashMap<>();

    private static class CellWeather {
        double rain;
        double windX;
        double windY;
        double temp;
    }

    public WeatherManager(Grid grid, Stage stage) {
        this.grid = grid;
        this.stage = stage;
    }

    @Override
    public void onWeatherEvent(WeatherEvent event) {
        // Convert world coordinates to a cell, if inside the grid
        Cell cell = fromWorldCoord(event.x, event.y);
        if (cell == null) return;
        weatherByCell.computeIfAbsent(cell, k -> new CellWeather());
        CellWeather cw = weatherByCell.get(cell);
        switch (event.attribute.toLowerCase()) {
            case "rain" -> cw.rain = event.value;
            case "windx" -> cw.windX = event.value;
            case "windy" -> cw.windY = event.value;
            case "temp" -> cw.temp = event.value;
            default -> {
            }
        }
        // Update effects list for that cell
        updateEffects(cell, cw);
        // Recalculate global strategies whenever a new event arrives
        updateStrategies();
    }

    private Cell fromWorldCoord(int wx, int wy) {
        int cols = grid.cells.length;
        int rows = grid.cells[0].length;
        int col = wx + cols / 2;
        int row = (rows / 2) - wy;
        if (col < 0 || col >= cols || row < 0 || row >= rows) return null;
        return grid.cells[col][row];
    }

    private void updateEffects(Cell cell, CellWeather cw) {
        List<WeatherEffect> list = new ArrayList<>();
        if (cw.rain > 0.2) {
            list.add(new RainEffect(cw.rain));
        }
        double dx = cw.windX * 2 - 1;
        double dy = cw.windY * 2 - 1;
        double windMag = Math.hypot(dx, dy);
        if (windMag > 0.02) {
            list.add(new WindEffect(dx, dy, Math.min(1.0, windMag)));
        }
        if (cw.temp > 0.5) {
            list.add(new HeatEffect(cw.temp));
        }
        effectsByCell.put(cell, list);
    }

    private void updateStrategies() {
        // If we have no weather data yet, do not adjust strategies
        if (weatherByCell.isEmpty()) return;

        Cell enemyCell = stage.getEnemyCell();
        CellWeather local = weatherByCell.get(enemyCell);
        if (local == null) {
            // no data: revert to default strategies
            stage.setEnemyStepStrategy(() -> {
                return stage.rng.nextDouble() < 0.8 ? 1 : 2;
            });
            stage.setEnemyChoiceStrategy((best, filtered) -> {
                List<Cell> pool = filtered.isEmpty() ? best : filtered;
                return pool.get(stage.rng.nextInt(pool.size()));
            });
            return;
        }
        double rain = local.rain;
        double temp = local.temp;
        double windMag = Math.hypot(local.windX * 2 - 1, local.windY * 2 - 1);
        // Step strategy based on rain and heat
        if (temp >= 0.5) {
            stage.setEnemyStepStrategy(() -> {
                return stage.rng.nextDouble() < 0.5 ? 0 : 1;
            });
        } else if (rain >= 0.2) {
            stage.setEnemyStepStrategy(() -> 1);
        } else {
            stage.setEnemyStepStrategy(() -> {
                return stage.rng.nextDouble() < 0.8 ? 1 : 2;
            });
        }
        // Choice strategy based on wind
        if (windMag >= 0.3) {
            stage.setEnemyChoiceStrategy((best, filtered) -> {
                List<Cell> choicePool = new ArrayList<>();
                choicePool.addAll(best);
                return choicePool.get(stage.rng.nextInt(choicePool.size()));
            });
        } else {
            stage.setEnemyChoiceStrategy((best, filtered) -> {
                List<Cell> pool = filtered.isEmpty() ? best : filtered;
                return pool.get(stage.rng.nextInt(pool.size()));
            });
        }
    }

    public void paint(Graphics g) {
        effectsByCell.forEach((cell, list) -> {
            for (WeatherEffect eff : list) {
                eff.paint(g, cell);
            }
        });
    }
}