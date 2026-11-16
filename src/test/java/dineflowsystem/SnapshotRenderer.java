package dineflowsystem;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Optional offscreen UI documentation renderer; does not control the desktop. */
public final class SnapshotRenderer {
  private static Component named(Container parent, String name) {
    for (Component child : parent.getComponents()) {
      if (name.equals(child.getName())) return child;
      if (child instanceof Container container) {
        Component result = named(container, name);
        if (result != null) return result;
      }
    }
    return null;
  }

  private static void layout(Container component) {
    if (component instanceof JScrollPane scroll
        && scroll.getViewport().getView() instanceof JTable table)
      scroll.setColumnHeaderView(table.getTableHeader());
    component.doLayout();
    for (Component child : component.getComponents())
      if (child instanceof Container container) layout(container);
  }

  public static void main(String[] args) throws Exception {
    Path output = Path.of(args.length > 0 ? args[0] : "docs/site/workspace.png");
    Files.createDirectories(output.toAbsolutePath().getParent());
    SwingUtilities.invokeAndWait(
        () -> {
          FlatLightLaf.setup();
          UIManager.put("defaultFont", new Font("SansSerif", Font.PLAIN, 14));
          CustomerDashboard panel = new CustomerDashboard(new Restaurant());
          ((JTextField) named(panel, "customerId")).setText("7");
          ((JTextField) named(panel, "customerName")).setText("Demo Guest");
          ((JTextField) named(panel, "customerContact")).setText("0123456789");
          JTable menu = (JTable) named(panel, "menuTable");
          menu.setRowSelectionInterval(0, 0);
          ((JButton) named(panel, "addItem")).doClick(0);
          menu.setRowSelectionInterval(1, 1);
          ((JButton) named(panel, "addItem")).doClick(0);
          panel.setSize(1180, 650);
          layout(panel);
          BufferedImage image = new BufferedImage(1180, 650, BufferedImage.TYPE_INT_RGB);
          Graphics2D graphics = image.createGraphics();
          panel.printAll(graphics);
          graphics.dispose();
          try {
            ImageIO.write(image, "png", output.toFile());
          } catch (Exception error) {
            throw new RuntimeException(error);
          }
        });
    System.out.println("Rendered Swing documentation image: " + output);
  }
}
