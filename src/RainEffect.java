import java.awt.Color;
import java.awt.Graphics;

// Visualises rainfall on a cell by drawing a semi‑transparent blue overlay
public class RainEffect implements WeatherEffect {
    private final double intensity;
    public RainEffect(double intensity) {
        this.intensity = Math.max(0.0, Math.min(1.0, intensity));
    }
    @Override
    public void paint(Graphics g, Cell cell) {
        int alpha = (int) (80 + 175 * intensity); // between 80 and 255
        Color c = new Color(0, 0, 255, alpha);
        Color old = g.getColor();
        g.setColor(c);
        g.fillRect(cell.x, cell.y, Cell.size, Cell.size);
        g.setColor(old);
    }
}