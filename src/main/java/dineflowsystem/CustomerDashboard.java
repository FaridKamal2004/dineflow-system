package dineflowsystem;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Ordering UI. All submitted data comes from the domain draft, not table cells. */
public final class CustomerDashboard extends JPanel {
  private final Restaurant restaurant;
  private final JTextField id = new JTextField(7),
      name = new JTextField(16),
      contact = new JTextField(14);
  private final DefaultTableModel menuModel = Ui.model("Meal", "Price (RM)"),
      basketModel = Ui.model("Item", "Price (RM)");
  private final JTable menu = Ui.table(menuModel), basket = Ui.table(basketModel);
  private final JLabel total = Ui.title("RM 0.00"), count = Ui.note("0 items in this order");
  private final JButton submit;
  private Order draft;
  private boolean busy;

  public CustomerDashboard(Restaurant restaurant) {
    this.restaurant = restaurant;
    setLayout(new BorderLayout(18, 18));
    setBackground(Ui.BG);
    setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    JPanel heading = new JPanel(new GridLayout(2, 1, 0, 7));
    heading.setOpaque(false);
    heading.add(Ui.title("Good food. Smooth service."));
    heading.add(Ui.note("Build an order, check the total, and send it to the kitchen."));
    JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
    form.setBorder(BorderFactory.createTitledBorder("Customer details"));
    Ui.label(form, "Customer ID", id);
    Ui.label(form, "Name", name);
    Ui.label(form, "Contact", contact);
    JButton load =
        Ui.button(
            "Find customer",
            () -> {
              if (busy) return;
              try {
                int customerId = Integer.parseInt(id.getText().trim());
                Ui.async(
                    this,
                    null,
                    () -> DatabaseOperations.getCustomerById(customerId),
                    customer -> {
                      if (customer == null)
                        JOptionPane.showMessageDialog(
                            this,
                            "No saved customer found. Enter their details to create one with the"
                                + " order.");
                      else {
                        name.setText(customer.getCustName());
                        contact.setText(customer.getCustContact());
                      }
                    });
              } catch (Exception error) {
                Ui.error(this, new IllegalArgumentException("Enter a positive customer ID."));
              }
            });
    form.add(load);
    JPanel top = new JPanel(new BorderLayout(10, 18));
    top.setOpaque(false);
    top.add(heading, BorderLayout.NORTH);
    top.add(form, BorderLayout.SOUTH);
    add(top, BorderLayout.NORTH);
    for (Meal meal : restaurant.getMeal())
      menuModel.addRow(new Object[] {meal.getMealName(), meal.price()});
    menu.setName("menuTable");
    basket.setName("basketTable");
    id.setName("customerId");
    name.setName("customerName");
    contact.setName("customerContact");
    JPanel menuPanel = new JPanel(new BorderLayout(10, 10));
    menuPanel.setBorder(BorderFactory.createTitledBorder("01 / Menu · Malaysian favourites"));
    menuPanel.add(new JScrollPane(menu), BorderLayout.CENTER);
    JButton add = Ui.button("+ Add selected meal", this::addItem);
    add.setName("addItem");
    menuPanel.add(add, BorderLayout.SOUTH);
    JPanel basketPanel = new JPanel(new BorderLayout(10, 10));
    basketPanel.setBorder(BorderFactory.createTitledBorder("02 / Current order"));
    basketPanel.add(new JScrollPane(basket), BorderLayout.CENTER);
    basketPanel.add(
        Ui.row(
            Ui.button("Remove selected", this::removeItem),
            Ui.button("Clear draft", this::clearDraft)),
        BorderLayout.SOUTH);
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, menuPanel, basketPanel);
    split.setResizeWeight(.5);
    split.setBorder(null);
    split.setDividerSize(12);
    split.setOpaque(false);
    add(split, BorderLayout.CENTER);
    submit = Ui.button("Submit to kitchen  →", this::submitOrder);
    submit.setName("submitOrder");
    submit.setBackground(Ui.GREEN);
    submit.setForeground(Color.WHITE);
    JPanel summary = new JPanel(new BorderLayout());
    summary.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
    summary.add(Ui.row(total, count), BorderLayout.WEST);
    summary.add(submit, BorderLayout.EAST);
    add(summary, BorderLayout.SOUTH);
    redraw();
  }

  private void addItem() {
    if (busy) return;
    try {
      if (menu.getSelectedRow() < 0)
        throw new IllegalArgumentException("Select a meal from the menu first.");
      if (draft == null) draft = new Order(Integer.parseInt(id.getText().trim()));
      draft.addItem(restaurant.getMeal()[menu.convertRowIndexToModel(menu.getSelectedRow())]);
      redraw();
    } catch (NumberFormatException error) {
      Ui.error(
          this, new IllegalArgumentException("Enter a positive whole-number customer ID first."));
    } catch (Exception error) {
      Ui.error(this, error);
    }
  }

  private void removeItem() {
    if (busy) return;
    try {
      if (draft == null || basket.getSelectedRow() < 0)
        throw new IllegalArgumentException("Select an item in the current order.");
      draft.removeItem(basket.convertRowIndexToModel(basket.getSelectedRow()));
      redraw();
    } catch (Exception error) {
      Ui.error(this, error);
    }
  }

  private void clearDraft() {
    if (busy) return;
    if (draft != null
        && draft.getItemCount() > 0
        && JOptionPane.showConfirmDialog(
                this, "Discard this unsent order?", "Clear draft", JOptionPane.YES_NO_OPTION)
            != JOptionPane.YES_OPTION) return;
    draft = null;
    redraw();
  }

  private void submitOrder() {
    if (busy) return;
    try {
      if (draft == null || draft.getItemCount() == 0)
        throw new IllegalArgumentException("Add at least one meal.");
      Customer customer =
          new Customer(Integer.parseInt(id.getText().trim()), name.getText(), contact.getText());
      Order submitted = draft;
      busy = true;
      setFieldsEditable(false);
      Ui.async(
          this,
          submit,
          () -> DatabaseOperations.submitOrder(customer, submitted),
          orderId -> {
            draft = null;
            redraw();
            Ui.async(
                this,
                null,
                () -> DatabaseOperations.receipt(orderId),
                receipt -> Ui.receipt(this, receipt));
          },
          () -> {
            busy = false;
            setFieldsEditable(true);
            redraw();
          });
    } catch (Exception error) {
      Ui.error(this, error);
    }
  }

  private void setFieldsEditable(boolean editable) {
    name.setEditable(editable);
    contact.setEditable(editable);
    id.setEditable(editable && (draft == null || draft.getItemCount() == 0));
  }

  private void redraw() {
    basketModel.setRowCount(0);
    if (draft != null)
      for (Meal meal : draft.items())
        basketModel.addRow(new Object[] {meal.getMealName(), meal.price()});
    total.setText("RM " + (draft == null ? "0.00" : draft.total().toPlainString()));
    int size = draft == null ? 0 : draft.getItemCount();
    count.setText(
        size + (size == 1 ? " item" : " items") + " · No tax / service charge configured");
    id.setEditable(!busy && size == 0);
    submit.setEnabled(!busy && size > 0);
  }
}
