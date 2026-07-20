package com.mycompany.pharmacypos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.AbstractCellEditor;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * The "Sales History" screen, opened from the Home dashboard.
 * Shows one row per invoice (newest first), with a running total, and can be
 * filtered by medicine name, invoice number, or sale date. Each row has a
 * View button that opens the full invoice (medicines, Dr Fee, Test Fee(s),
 * Grand Total) via the same InvoicePrinter screen used after a sale.
 */
public class SalesHistoryFrame {

    private static final int INVOICE_NO_COLUMN = 0;
    private static final int VIEW_COLUMN = 6;

    private final SalesDAO salesDAO = new SalesDAO();

    private JFrame salesFrame;
    private DefaultTableModel model;
    private JLabel totalSalesLabel;

    public void show() {
        salesFrame = new JFrame("Sales History");
        salesFrame.setSize(1050, 640);
        salesFrame.setLocationRelativeTo(null);
        salesFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        salesFrame.setLayout(null);
        salesFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        JLabel searchLbl = new JLabel("Search (Medicine / Invoice No / Date):");
        searchLbl.setBounds(20, 15, 260, 25);
        salesFrame.add(searchLbl);

        JTextField searchField = new JTextField();
        searchField.setBounds(290, 12, 300, 30);
        salesFrame.add(searchField);

        String[] columns = {
            "Invoice Number", "Medicine Fee", "Test Fee", "Doctor Fee", "Total Bill", "Sale Date", "View Invoice"
        };

        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == VIEW_COLUMN;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.getColumnModel().getColumn(VIEW_COLUMN).setCellRenderer(new ViewButtonRenderer());
        table.getColumnModel().getColumn(VIEW_COLUMN).setCellEditor(new ViewButtonEditor(table));

        JScrollPane sp = new JScrollPane(table);
        sp.setBounds(20, 55, 1000, 480);
        salesFrame.add(sp);

        totalSalesLabel = new JLabel("Total Sales: Rs. 0.00");
        totalSalesLabel.setBounds(20, 555, 400, 30);
        totalSalesLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        salesFrame.add(totalSalesLabel);

        ResponsiveUtil.makeResponsive(salesFrame, sp, 20, 130, totalSalesLabel);

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                loadHistory(searchField.getText().trim());
            }
        });

        loadHistory("");

        salesFrame.setVisible(true);
    }

    private void loadHistory(String searchText) {
        model.setRowCount(0);
        try {
            double totalSales = salesDAO.loadSalesHistoryByInvoice(model, searchText);
            totalSalesLabel.setText("Total Sales: Rs. " + String.format("%.2f", totalSales));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(salesFrame, "Error loading sales history: " + ex.getMessage());
        }
    }

    /** Plain "View" button look for the action column. */
    private static class ViewButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        ViewButtonRenderer() {
            setText("View");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            return this;
        }
    }

    /** Opens the full invoice (same screen used right after completing a sale)
     *  for the invoice number on the clicked row. */
    private static class ViewButtonEditor extends DefaultCellEditorButton {
        private final JTable table;
        private String invoiceNo;

        ViewButtonEditor(JTable table) {
            super("View");
            this.table = table;
            button.addActionListener(e -> {
                fireEditingStopped();
                if (invoiceNo != null && !invoiceNo.isEmpty()) {
                    InvoicePrinter.printInvoice(invoiceNo);
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                       int row, int column) {
            invoiceNo = String.valueOf(table.getModel().getValueAt(row, INVOICE_NO_COLUMN));
            return button;
        }
    }

    /** Minimal reusable base for a single-click table button editor. */
    private static class DefaultCellEditorButton extends AbstractCellEditor
            implements javax.swing.table.TableCellEditor {
        protected final JButton button;

        DefaultCellEditorButton(String label) {
            button = new JButton(label);
        }

        @Override
        public Object getCellEditorValue() {
            return button.getText();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                       int row, int column) {
            return button;
        }
    }
}
