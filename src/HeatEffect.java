import java.awt.Color;
import java.awt.Graphics;

/*
  Visualises temperature by drawing a red overlay on the cell. Higher
  temperatures produce deeper, more opaque red. When temperature is below
  0.5 the effect is omitted entirely.
 */
public class HeatEffect implements WeatherEffect {
    private final double temperature;
    public HeatEffect(double temperature) {
        this.temperature = Math.max(0.0, Math.min(1.0, temperature));
    }
    @Override
    public void paint(Graphics g, Cell cell) {
        int alpha = (int) (60 + 195 * temperature);
        Color c = new Color(255, 0, 0, alpha);
        Color old = g.getColor();
        g.setColor(c);
        g.fillRect(cell.x, cell.y, Cell.size, Cell.size);
        g.setColor(old);
    }
}