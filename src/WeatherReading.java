import java.util.Optional;

public final class WeatherReading {
    public final long timestamp;
    public final String attribute;
    public final int x;
    public final int y;
    public final double value;

    public WeatherReading(long ts, String attr, int x, int y, double v) {
        this.timestamp = ts;
        this.attribute = attr;
        this.x = x;
        this.y = y;
        this.value = v;
    }

    public static Optional<WeatherReading> parse(String line) {
        if (line == null || line.isBlank()) return Optional.empty();
        String[] parts = line.trim().split("\\s+");
        if (parts.length != 5) return Optional.empty();
        try {
            long ts = Long.parseLong(parts[0]);
            String attr = parts[1].toLowerCase();
            int wx = Integer.parseInt(parts[2]);
            int wy = Integer.parseInt(parts[3]);
            double v = Double.parseDouble(parts[4]);
            return Optional.of(new WeatherReading(ts, attr, wx, wy, v));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return attribute + "@" + x + "," + y + "=" + value;
    }
}