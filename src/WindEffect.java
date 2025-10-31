import java.awt.Color;
import java.awt.Graphics;

public class WindEffect implements WeatherEffect {
    private final double dx;
    private final double dy;
    private final double intensity;
    public WindEffect(double dx, double dy, double intensity) {
        // normalise so arrow length corresponds to intensity
        double mag = Math.hypot(dx, dy);
        if (mag > 0.001) {
            this.dx = dx / mag;
            this.dy = dy / mag;
        } else {
            this.dx = 0;
            this.dy = 0;
        }
        this.intensity = Math.max(0.0, Math.min(1.0, intensity));
    }
    @Override
    public void paint(Graphics g, Cell cell) {
        int centerX = cell.x + Cell.size / 2;
        int centerY = cell.y + Cell.size / 2;
        // Scale the arrow length up so even gentle breezes are visible.
        int length = (int) (Cell.size * intensity);
        int endX = (int) (centerX + dx * length);
        int endY = (int) (centerY - dy * length); // invert y for screen coords
        Color old = g.getColor();
        // Fill the background with a light aqua colour
        int bgAlpha = (int) (40 + 100 * intensity);
        g.setColor(new Color(180, 230, 255, Math.max(0, Math.min(255, bgAlpha))));
        g.fillRect(cell.x, cell.y, Cell.size, Cell.size);
        // Draw the wind arrow in a stronger cyan hue
        int alpha = (int) (100 + 155 * intensity);
        g.setColor(new Color(0, 200, 255, Math.max(0, Math.min(255, alpha))));
        g.drawLine(centerX, centerY, endX, endY);
        // Draw arrow head.  Size scales with intensity
        int arrSize = (int) Math.max(3, 6 * intensity);
        int hx = (int) (endX - dx * arrSize + dy * arrSize);
        int hy = (int) (endY + dx * arrSize + dy * arrSize);
        int hx2 = (int) (endX - dx * arrSize - dy * arrSize);
        int hy2 = (int) (endY - dx * arrSize + dy * arrSize);
        g.drawLine(endX, endY, hx, hy);
        g.drawLine(endX, endY, hx2, hy2);
        g.setColor(old);
    }
}