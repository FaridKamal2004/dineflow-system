package dineflowsystem;

import java.awt.*;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

public final class OrderBoard extends JPanel {
  private final DefaultTableModel model =
      Ui.model("Order #", "Customer", "Status", "Total (RM)", "Created");
  private final JTable table = Ui.table(model);
  private final JComboBox<String> filter =
      new JComboBox<>(
          new String[] {"All orders", "QUEUED", "PREPARING", "READY", "SERVED", "CANCELLED"});
  private final JTextField search = new JTextField(16);
  private final JLabel summary = Ui.note("Loading orders…");
  private List<DatabaseOperations.Ticket> tickets = List.of(), visible = List.of();
  private final JButton advance, cancel;

  public OrderBoard() {
    setLayout(new BorderLayout(16, 16));
    setBackground(Ui.BG);
    setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    JPanel top = new JPanel(new BorderLayout(10, 10));
    top.setOpaque(false);
    top.add(Ui.title("Every order. One clear flow."), BorderLayout.NORTH);
    JPanel filters =
        Ui.row(
            Ui.note("Status"),
            filter,
            Ui.note("Search name / order #"),
            search,
            Ui.button("Refresh", this::refresh));
    search.getAccessibleContext().setAccessibleName("Search orders");
    filter.getAccessibleContext().setAccessibleName("Order status filter");
    top.add(filters, BorderLayout.CENTER);
    top.add(summary, BorderLayout.SOUTH);
    add(top, BorderLayout.NORTH);
    add(new JScrollPane(table), BorderLayout.CENTER);
    advance = Ui.button("Advance status", this::advance);
    cancel = Ui.button("Cancel order", this::cancel);
    add(Ui.row(advance, cancel, Ui.button("View receipt", this::receipt)), BorderLayout.SOUTH);
    filter.addActionListener(event -> render());
    search
        .getDocument()
        .addDocumentListener(
            new DocumentListener() {
              public void insertUpdate(DocumentEvent e) {
                render();
              }

              public void removeUpdate(DocumentEvent e) {
                render();
              }

              public void changedUpdate(DocumentEvent e) {
                render();
              }
            });
    table.getSelectionModel().addListSelectionListener(event -> updateButtons());
    updateButtons();
  }

  public void refresh() {
    Ui.async(
        this,
        null,
        DatabaseOperations::listOrders,
        list -> {
          tickets = list;
          render();
        });
  }

  private void render() {
    String query = search.getText().trim().toLowerCase(Locale.ROOT),
        status = (String) filter.getSelectedItem();
    visible =
        tickets.stream()
            .filter(
                ticket ->
                    ("All orders".equals(status) || ticket.status().name().equals(status))
                        && (ticket.customerName().toLowerCase(Locale.ROOT).contains(query)
                            || String.valueOf(ticket.id()).contains(query)))
            .toList();
    model.setRowCount(0);
    for (var ticket : visible)
      model.addRow(
          new Object[] {
            ticket.id(),
            ticket.customerName(),
            ticket.status(),
            ticket.total(),
            ticket.createdAt().withNano(0)
          });
    long active =
        tickets.stream()
            .filter(t -> t.status() != OrderStatus.SERVED && t.status() != OrderStatus.CANCELLED)
            .count();
    summary.setText(
        tickets.size()
            + " orders recorded · "
            + active
            + " active · "
            + visible.size()
            + " shown · Refresh to retrieve changes");
    updateButtons();
  }

  private DatabaseOperations.Ticket selected() {
    int row = table.getSelectedRow();
    if (row < 0) throw new IllegalArgumentException("Select an order first.");
    return visible.get(table.convertRowIndexToModel(row));
  }

  private void updateButtons() {
    int row = table.getSelectedRow();
    OrderStatus status =
        row < 0 || row >= visible.size()
            ? null
            : visible.get(table.convertRowIndexToModel(row)).status();
    advance.setEnabled(status != null && status.next() != null);
    cancel.setEnabled(status != null && status.canMoveTo(OrderStatus.CANCELLED));
    advance.setText(
        status != null && status.next() != null
            ? "Mark " + status.next().name().toLowerCase(Locale.ROOT) + " →"
            : "Advance status");
  }

  private void advance() {
    try {
      var ticket = selected();
      Ui.async(
          this,
          advance,
          () -> {
            DatabaseOperations.changeStatus(ticket.id(), ticket.status(), ticket.status().next());
            return DatabaseOperations.listOrders();
          },
          list -> {
            tickets = list;
            render();
          });
    } catch (Exception error) {
      Ui.error(this, error);
    }
  }

  private void cancel() {
    try {
      var ticket = selected();
      if (JOptionPane.showConfirmDialog(
              this,
              "Cancel order #" + ticket.id() + "? It will remain in the order history.",
              "Cancel order",
              JOptionPane.YES_NO_OPTION)
          != JOptionPane.YES_OPTION) return;
      Ui.async(
          this,
          cancel,
          () -> {
            DatabaseOperations.changeStatus(ticket.id(), ticket.status(), OrderStatus.CANCELLED);
            return DatabaseOperations.listOrders();
          },
          list -> {
            tickets = list;
            render();
          });
    } catch (Exception error) {
      Ui.error(this, error);
    }
  }

  private void receipt() {
    try {
      int id = selected().id();
      Ui.async(this, null, () -> DatabaseOperations.receipt(id), text -> Ui.receipt(this, text));
    } catch (Exception error) {
      Ui.error(this, error);
    }
  }
}
