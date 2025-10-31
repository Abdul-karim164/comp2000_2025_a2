import java.awt.*;

public abstract class Item extends Entity implements Collectable {
    protected Item(Cell loc, Grid grid) { super(loc, grid); }
    @Override protected void updatePolygons() {}
    @Override public void paint(Graphics g) {
        int s = Math.max(10, loc.width/2);
        int x = loc.x + (loc.width - s)/2, y = loc.y + (loc.height - s)/2;
        g.setColor(color); g.fillRect(x,y,s,s);
        g.setColor(Color.BLACK); g.drawRect(x,y,s,s);
    }
}