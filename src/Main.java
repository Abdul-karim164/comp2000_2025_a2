import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


public class Main extends JFrame {
  private Canvas canvas;

  public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> new Main().start());
  }

  private void start() {
      setTitle("COMP2000 – Weather Rabbit");
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      canvas = new Canvas();
      setContentPane(canvas);
      pack();
      setLocationRelativeTo(null);
      setVisible(true);

      // keys to stage
      canvas.setFocusable(true);
      canvas.requestFocusInWindow();
      canvas.addKeyListener(new java.awt.event.KeyAdapter() {
          @Override public void keyPressed(java.awt.event.KeyEvent e) {
              canvas.getStage().handleKey(e.getKeyCode());
              canvas.repaint();
          }
      });

      // repaint timer
      new javax.swing.Timer(500, ev -> canvas.repaint()).start();
  }

  private static final class Canvas extends JPanel {
      private final Stage stage = new Stage();
      Canvas() {
          setPreferredSize(new Dimension(1024, 720));
          setDoubleBuffered(true);
      }
      Stage getStage() { return stage; }
      @Override protected void paintComponent(Graphics g) {
          super.paintComponent(g);
          stage.paint(g, getMousePosition());
      }
  }
}