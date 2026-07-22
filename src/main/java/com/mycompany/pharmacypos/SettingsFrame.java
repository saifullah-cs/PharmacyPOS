package com.mycompany.pharmacypos;

import javax.swing.*;
import java.awt.*;

/**
 * Settings menu, opened from the Home dashboard. Shows a clean list of
 * available settings; each one opens its own dedicated screen instead of
 * everything being crammed onto one long page. Add new settings by adding
 * another addOption(...) call below - no need to touch the rest of the layout.
 */
public class SettingsFrame {

    public void show() {
        JFrame frame = new JFrame("Settings");
        frame.setSize(480, 420);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        JLabel header = new JLabel("Settings");
        header.setFont(new Font("Segoe UI", Font.BOLD, 24));
        header.setForeground(new Color(0, 102, 102));
        header.setBorder(BorderFactory.createEmptyBorder(20, 25, 10, 25));
        frame.add(header, BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);
        listPanel.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));

        addOption(listPanel, "Change User Password",
                "Update the login password for any user account.",
                () -> new ChangePasswordFrame().show());

        addOption(listPanel, "Low Stock Alert Threshold",
                "Set the quantity below which a medicine is flagged as low stock.",
                () -> new LowStockThresholdFrame().show());

        addOption(listPanel, "Default Consultation Fee",
                "View and update the default Doctor Fee added to each sale.",
                () -> new DoctorFeeFrame().show());

        addOption(listPanel, "Test Management",
                "Add, edit, or remove tests and their default prices.",
                () -> new TestManagementFrame().show());

        addOption(listPanel, "Distributor Management",
                "Add, edit, or remove distributors and their contact notes.",
                () -> new DistributorManagementFrame().show());

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        frame.add(scrollPane, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    /** One row in the settings list: a title, a short description, and a
     *  ">" affordance, all clickable to open that setting's own screen. */
    private void addOption(JPanel parent, String title, String description, Runnable onOpen) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(14, 8, 14, 8)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(Color.WHITE);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(titleLbl);

        JLabel descLbl = new JLabel(description);
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLbl.setForeground(new Color(120, 120, 120));
        descLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(descLbl);

        row.add(textPanel, BorderLayout.CENTER);

        JLabel arrow = new JLabel("\u203A");
        arrow.setFont(new Font("Segoe UI", Font.BOLD, 22));
        arrow.setForeground(new Color(0, 150, 136));
        row.add(arrow, BorderLayout.EAST);

        row.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                onOpen.run();
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                row.setBackground(new Color(245, 250, 250));
                textPanel.setBackground(new Color(245, 250, 250));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                row.setBackground(Color.WHITE);
                textPanel.setBackground(Color.WHITE);
            }
        });

        parent.add(row);
    }
}
