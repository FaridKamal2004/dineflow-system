package dineflowsystem;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class StaffDashboard extends JPanel {
  private final JTextField id = new JTextField(12),
      name = new JTextField(16),
      contact = new JTextField(16),
      hours = new JTextField("0", 12);
  private final JLabel salary = Ui.title("RM 0.00");
  private final DefaultTableModel model =
      Ui.model("Staff ID", "Name", "Role", "Hours", "Rate (RM)", "Pay (RM)");
  private final JTable table = Ui.table(model);
  private List<Staff> records = List.of();
  private final JButton save;

  public StaffDashboard() {
    setLayout(new BorderLayout(16, 16));
    setBackground(Ui.BG);
    setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    JPanel heading = new JPanel(new GridLayout(2, 1, 0, 8));
    heading.setOpaque(false);
    heading.add(Ui.title("A good team, in balance."));
    heading.add(Ui.note("Local staff directory and a transparent demonstration pay calculation."));
    add(heading, BorderLayout.NORTH);
    JPanel form = new JPanel(new GridLayout(0, 2, 14, 14));
    form.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
    Ui.label(form, "Staff ID (W1 / R1 / F1)", id);
    Ui.label(form, "Name", name);
    Ui.label(form, "Contact number", contact);
    Ui.label(form, "Monthly hours (0–744)", hours);
    form.add(Ui.note("Calculated demonstration pay"));
    form.add(salary);
    JButton calculate =
        Ui.button(
            "Calculate pay",
            () -> {
              try {
                salary.setText("RM " + readStaff().salary().toPlainString());
              } catch (Exception error) {
                Ui.error(this, error);
              }
            });
    save =
        Ui.button(
            "Save / update staff",
            () -> {
              try {
                Staff staff = readStaff();
                Ui.async(
                    this,
                    saveButton(),
                    () -> {
                      DatabaseOperations.saveStaff(staff);
                      return DatabaseOperations.listStaff();
                    },
                    list -> {
                      showRecords(list);
                      salary.setText("RM " + staff.salary().toPlainString());
                    });
              } catch (Exception error) {
                Ui.error(this, error);
              }
            });
    JPanel edit = new JPanel(new BorderLayout(10, 10));
    edit.add(form, BorderLayout.CENTER);
    edit.add(Ui.row(calculate, save, Ui.button("New staff", this::clear)), BorderLayout.SOUTH);
    JPanel directory = new JPanel(new BorderLayout(10, 10));
    directory.setBorder(BorderFactory.createTitledBorder("Saved team · select a row to edit"));
    directory.add(new JScrollPane(table), BorderLayout.CENTER);
    directory.add(Ui.row(Ui.button("Refresh directory", this::refresh)), BorderLayout.SOUTH);
    JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, edit, directory);
    split.setResizeWeight(.4);
    split.setBorder(null);
    split.setDividerSize(12);
    add(split, BorderLayout.CENTER);
    JTextArea policy =
        new JTextArea(
            "Demo policy from the original coursework: Waiter RM 10/h · Cashier RM 15/h · Chef RM"
                + " 20/h. First 160 hours at base rate; later hours at 1.5×. This is not a"
                + " statutory payroll, tax, or benefits engine.");
    policy.setEditable(false);
    policy.setLineWrap(true);
    policy.setWrapStyleWord(true);
    policy.setOpaque(false);
    policy.setForeground(Ui.MUTED);
    policy.setRows(2);
    add(policy, BorderLayout.SOUTH);
    table
        .getSelectionModel()
        .addListSelectionListener(
            event -> {
              if (!event.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                Staff staff = records.get(table.convertRowIndexToModel(table.getSelectedRow()));
                id.setText(staff.getStaffID());
                name.setText(staff.getStaffName());
                contact.setText(staff.getStaffContact());
                hours.setText(String.valueOf(staff.getHourWorkMonth()));
                salary.setText("RM " + staff.salary().toPlainString());
                id.setEditable(false);
              }
            });
  }

  private JButton saveButton() {
    return save;
  }

  private Staff readStaff() {
    Staff staff =
        new StaffDeployPattern().createStaff(id.getText(), name.getText(), contact.getText());
    try {
      staff.setHourWorkMonth(Double.parseDouble(hours.getText().trim()));
    } catch (NumberFormatException error) {
      throw new IllegalArgumentException("Enter a valid number of monthly hours.");
    }
    return staff;
  }

  private void clear() {
    table.clearSelection();
    id.setEditable(true);
    id.setText("");
    name.setText("");
    contact.setText("");
    hours.setText("0");
    salary.setText("RM 0.00");
  }

  public void refresh() {
    Ui.async(this, null, DatabaseOperations::listStaff, this::showRecords);
  }

  private void showRecords(List<Staff> list) {
    records = list;
    model.setRowCount(0);
    for (Staff staff : list)
      model.addRow(
          new Object[] {
            staff.getStaffID(),
            staff.getStaffName(),
            staff.role(),
            staff.getHourWorkMonth(),
            staff.getRatePerHour(),
            staff.salary()
          });
  }
}
