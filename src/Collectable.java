import java.awt.Graphics;
public interface Collectable {
    void paint(Graphics g);
    boolean canBeCollectedBy(Actor a);
}