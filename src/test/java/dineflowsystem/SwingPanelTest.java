package dineflowsystem;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;

class SwingPanelTest {
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

  @Test
  void basketUiTracksMoreThanTenItemsAndLocksIdentity() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          CustomerDashboard panel = new CustomerDashboard(new Restaurant());
          JTextField id = (JTextField) named(panel, "customerId");
          id.setText("42");
          JTable menu = (JTable) named(panel, "menuTable"),
              basket = (JTable) named(panel, "basketTable");
          menu.setRowSelectionInterval(0, 0);
          JButton add = (JButton) named(panel, "addItem");
          for (int i = 0; i < 12; i++) add.doClick(0);
          assertEquals(12, basket.getRowCount());
          assertFalse(id.isEditable());
          assertTrue(((JButton) named(panel, "submitOrder")).isEnabled());
        });
  }

  @Test
  void emptyOrderCannotBeSubmitted() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          CustomerDashboard panel = new CustomerDashboard(new Restaurant());
          assertFalse(((JButton) named(panel, "submitOrder")).isEnabled());
          assertEquals(3, ((JTable) named(panel, "menuTable")).getRowCount());
        });
  }

  @Test
  void staffAndBoardPanelsConstructWithoutNativeWindows() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          assertDoesNotThrow(StaffDashboard::new);
          assertDoesNotThrow(OrderBoard::new);
        });
  }
}
