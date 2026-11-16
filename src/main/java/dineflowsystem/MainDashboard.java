package dineflowsystem;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.*;
import javax.swing.*;

public final class MainDashboard extends JFrame {
  public MainDashboard() {
    super("DineFlow · Restaurant workspace");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(1200, 820);
    setMinimumSize(new Dimension(1000, 720));
    setLocationRelativeTo(null);
    JPanel root = new JPanel(new BorderLayout());
    root.setBackground(Ui.BG);
    setContentPane(root);
    JPanel header = new JPanel(new BorderLayout());
    header.setBackground(Ui.GREEN);
    header.setBorder(BorderFactory.createEmptyBorder(23, 30, 23, 30));
    JLabel brand = new JLabel("dineflow / restaurant workspace");
    brand.setFont(new Font("SansSerif", Font.BOLD, 22));
    brand.setForeground(Color.WHITE);
    header.add(brand, BorderLayout.WEST);
    JLabel badge = new JLabel("LOCAL DEMO  •  JAVA 17  •  EMBEDDED DATABASE");
    badge.setForeground(new Color(217, 235, 215));
    badge.setFont(new Font("SansSerif", Font.PLAIN, 11));
    header.add(badge, BorderLayout.EAST);
    root.add(header, BorderLayout.NORTH);
    JTabbedPane tabs = new JTabbedPane();
    tabs.putClientProperty("JTabbedPane.tabHeight", 48);
    tabs.putClientProperty("JTabbedPane.tabType", "underlined");
    CustomerDashboard ordering = new CustomerDashboard(new Restaurant());
    OrderBoard board = new OrderBoard();
    StaffDashboard staff = new StaffDashboard();
    tabs.addTab("  New order  ", ordering);
    tabs.addTab("  Order board  ", board);
    tabs.addTab("  Team & pay  ", staff);
    tabs.addTab("  About  ", about());
    tabs.addChangeListener(
        event -> {
          if (tabs.getSelectedComponent() == board) board.refresh();
          if (tabs.getSelectedComponent() == staff) staff.refresh();
        });
    root.add(tabs, BorderLayout.CENTER);
    JPanel footer =
        Ui.row(
            Ui.note(
                "Independent coursework enhancement  /  Local records only  /  No payment"
                    + " processing or access control"));
    footer.setBorder(BorderFactory.createEmptyBorder(2, 20, 8, 20));
    root.add(footer, BorderLayout.SOUTH);
  }

  private JPanel about() {
    JPanel panel = Ui.panel();
    panel.add(Ui.title("From coursework to a working service flow."), BorderLayout.NORTH);
    JTextArea text =
        new JTextArea(
            "DineFlow is an independent portfolio enhancement of a university group Java OOP"
                + " project.\n\n"
                + "WHAT WORKS\n"
                + "• Validated customer ordering, exact decimal totals, stored receipts.\n"
                + "• Kitchen workflow: queued → preparing → ready → served; cancellation before"
                + " ready.\n"
                + "• Staff role factory, local staff records, and demonstration pay calculations.\n"
                + "• Embedded H2 storage, atomic order submission, rollback and optimistic status"
                + " checks.\n\n"
                + "DATA & SCOPE\n"
                + "Data is stored locally in ./data/dineflow.mv.db by default. Back up this file"
                + " with the application closed. Use -Ddineflow.db=/absolute/path/dineflow to"
                + " choose another location. No original MySQL data is migrated automatically.\n\n"
                + "This is a single-workstation demo with no authentication, role-based"
                + " authorization, tax engine, payments, cloud sync, or production hardening. Do"
                + " not use it for real customer or employee records. Payroll rates and overtime"
                + " rules reproduce the coursework policy; they do not establish legal"
                + " entitlements.\n\n"
                + "Developed with AI assistance. Credit belongs to the original group for the"
                + " coursework concept and source. Review the repository architecture, tests, and"
                + " interview guide before presenting it.");
    text.setEditable(false);
    text.setLineWrap(true);
    text.setWrapStyleWord(true);
    text.setFont(new Font("SansSerif", Font.PLAIN, 16));
    text.setForeground(Ui.INK);
    text.setBackground(Ui.BG);
    text.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
    panel.add(new JScrollPane(text), BorderLayout.CENTER);
    return panel;
  }

  public static void main(String[] args) {
    try {
      DatabaseConnection.initialize();
    } catch (Exception error) {
      SwingUtilities.invokeLater(() -> Ui.error(null, error));
      return;
    }
    SwingUtilities.invokeLater(
        () -> {
          FlatLightLaf.setup();
          UIManager.put("defaultFont", new Font("SansSerif", Font.PLAIN, 14));
          UIManager.put("Button.arc", 10);
          UIManager.put("Component.arc", 8);
          UIManager.put("TextComponent.arc", 8);
          new MainDashboard().setVisible(true);
        });
  }
}
