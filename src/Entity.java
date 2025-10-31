import java.awt.*;
import java.util.ArrayList;

public abstract class Entity {
    protected final Grid grid;
    protected Cell loc;
    protected Color color = Color.GRAY;
    protected final ArrayList<Polygon> display = new ArrayList<>();

    protected Entity(Cell loc, Grid grid) {
        this.loc = loc;
        this.grid = grid;
    }
    protected abstract void updatePolygons();

    // draw the entity
    public void paint(Graphics g) {
        g.setColor(color);
        for (Polygon p : display) ((Graphics2D) g).fill(p);
        g.setColor(Color.BLACK);
        for (Polygon p : display) ((Graphics2D) g).draw(p);
    }

    public Cell getCell() { return loc; }
    public void moveTo(Cell c) { this.loc = c; updatePolygons(); }
}
