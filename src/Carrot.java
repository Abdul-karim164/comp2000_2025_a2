import java.awt.Color;
public final class Carrot extends Item {
    public Carrot(Cell c, Grid g) {
        super(c, g);
        this.color = new Color(255,120,0);
    }
    @Override public boolean canBeCollectedBy(Actor a) {
        return a instanceof Rabbit;
    }
}