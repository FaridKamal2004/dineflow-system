package dineflowsystem;

import java.awt.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

final class Ui {
  static final Color INK = new Color(29, 45, 38),
      GREEN = new Color(39, 89, 62),
      MUTED = new Color(98, 110, 102),
      BG = new Color(245, 246, 241);

  private Ui() {}

  static JLabel title(String text) {
    JLabel label = new JLabel(text);
    label.setFont(new Font("SansSerif", Font.BOLD, 26));
    label.setForeground(INK);
    return label;
  }

  static JLabel note(String text) {
    JLabel label = new JLabel(text);
    label.setForeground(MUTED);
    return label;
  }

  static JButton button(String text, Runnable action) {
    JButton button = new JButton(text);
    button.addActionListener(event -> action.run());
    return button;
  }

  static JPanel row(Component... children) {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
    panel.setOpaque(false);
    for (Component child : children) panel.add(child);
    return panel;
  }

  static JPanel panel() {
    JPanel panel = new JPanel(new BorderLayout(16, 16));
    panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    panel.setBackground(BG);
    return panel;
  }

  static DefaultTableModel model(String... headers) {
    return new DefaultTableModel(headers, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };
  }

  static JTable table(DefaultTableModel model) {
    JTable table = new JTable(model);
    table.setRowHeight(36);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setFillsViewportHeight(true);
    table.getTableHeader().setReorderingAllowed(false);
    return table;
  }

  static void label(JPanel form, String text, JComponent input) {
    JLabel label = new JLabel(text);
    label.setLabelFor(input);
    input.getAccessibleContext().setAccessibleName(text);
    form.add(label);
    form.add(input);
  }

  static void error(Component parent, Exception error) {
    String message =
        error instanceof java.sql.SQLException
            ? "The local database operation failed. Your draft has been kept. Check file"
                  + " permissions or close other DineFlow instances."
            : error.getMessage();
    JTextArea text = new JTextArea(message == null ? "Operation failed." : message);
    text.setEditable(false);
    text.setLineWrap(true);
    text.setWrapStyleWord(true);
    text.setColumns(45);
    text.setRows(3);
    text.setBackground(UIManager.getColor("Panel.background"));
    JOptionPane.showMessageDialog(
        parent, text, "Could not complete operation", JOptionPane.WARNING_MESSAGE);
  }

  /** Keeps JDBC work off Swing's event dispatch thread. */
  static <T> void async(Component parent, JButton trigger, Callable<T> work, Consumer<T> success) {
    async(parent, trigger, work, success, () -> {});
  }

  static <T> void async(
      Component parent, JButton trigger, Callable<T> work, Consumer<T> success, Runnable finished) {
    if (trigger != null) trigger.setEnabled(false);
    new SwingWorker<T, Void>() {
      @Override
      protected T doInBackground() throws Exception {
        return work.call();
      }

      @Override
      protected void done() {
        if (trigger != null) trigger.setEnabled(true);
        try {
          success.accept(get());
        } catch (Exception error) {
          Throwable cause =
              error instanceof java.util.concurrent.ExecutionException ? error.getCause() : error;
          Ui.error(parent, cause instanceof Exception e ? e : new Exception(cause));
        } finally {
          finished.run();
        }
      }
    }.execute();
  }

  static void receipt(Component parent, String text) {
    JTextArea area = new JTextArea(text, 22, 48);
    area.setEditable(false);
    area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
    area.setCaretPosition(0);
    JScrollPane scroll = new JScrollPane(area);
    Object[] choices = {"Print", "Close"};
    if (JOptionPane.showOptionDialog(
            parent,
            scroll,
            "Receipt",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            choices,
            choices[1])
        == 0) {
      try {
        area.print();
      } catch (Exception error) {
        Ui.error(parent, error);
      }
    }
  }
}
