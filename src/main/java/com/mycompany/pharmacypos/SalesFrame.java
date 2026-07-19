package com.mycompany.pharmacypos;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class SalesFrame {
    JFrame frame;

    JButton searchBtn;
    JButton addBtn;
    JButton removeBtn;
    JButton clearBtn;
    JButton completeSaleBtn;
    JButton printBtn;
    JTable billTable;
    DefaultTableModel model;
    JLabel totalLabel;
    JTextField qtyField;
    JLabel medicineLabel;
    JLabel stockLabel;
    JLabel priceLabel;
    JTextField drFeeField;

    // Tests section
    JComboBox<String> testBox;
    JTable testsTable;
    DefaultTableModel testsModel;
    JButton removeTestBtn;
    private List<TestDAO.TestInfo> availableTests;

    // Medicine search field + its own floating suggestion list
    // (replaces the old editable JComboBox, which had a built-in bug:
    // arrow keys immediately committed a selection instead of just
    // moving a highlight, making it impossible to reach later suggestions)
    JTextField searchField;
    private JWindow suggestionPopup;
    private JList<String> suggestionList;
    private DefaultListModel<String> suggestionListModel;

    private final SalesDAO salesDAO = new SalesDAO();
    private final AppSettingsDAO appSettingsDAO = new AppSettingsDAO();
    private final TestDAO testDAO = new TestDAO();

    public SalesFrame() {
        frame = new JFrame("Pharmacy POS - Sales");
        frame.setSize(1000, 750);
        frame.setLayout(null);
        frame.setLocationRelativeTo(null);

        JLabel title = new JLabel("Sales / Billing");
        title.setFont(new Font("Arial",Font.BOLD,22));
        title.setBounds(390,15,250,30);
        frame.add(title);

        JLabel searchLbl = new JLabel("Medicine");
        searchLbl.setBounds(30,60,100,28);
        frame.add(searchLbl);

        // Narrower search box, with the Search button pulled in right next to it
        searchField = new JTextField();
        searchField.setBounds(120,60,180,28);
        frame.add(searchField);

        setupSuggestionPopup();

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { SwingUtilities.invokeLater(SalesFrame.this::showSuggestions); }
            @Override
            public void removeUpdate(DocumentEvent e) { SwingUtilities.invokeLater(SalesFrame.this::showSuggestions); }
            @Override
            public void changedUpdate(DocumentEvent e) { SwingUtilities.invokeLater(SalesFrame.this::showSuggestions); }
        });

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!suggestionPopup.isVisible()) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        searchBtn.doClick();
                        qtyField.requestFocus();
                    }
                    return;
                }

                int size = suggestionListModel.size();
                if (size == 0) return;
                int selected = suggestionList.getSelectedIndex();

                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    e.consume();
                    int next = (selected + 1) % size;
                    suggestionList.setSelectedIndex(next);
                    suggestionList.ensureIndexIsVisible(next);
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    e.consume();
                    int prev = (selected - 1 + size) % size;
                    suggestionList.setSelectedIndex(prev);
                    suggestionList.ensureIndexIsVisible(prev);
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    commitSelectedSuggestion();
                    searchBtn.doClick();
                    qtyField.requestFocus();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    suggestionPopup.setVisible(false);
                }
            }
        });

        searchBtn = new JButton("Search");
        searchBtn.setBounds(305,60,85,28);
        frame.add(searchBtn);

        medicineLabel = new JLabel("Medicine : -");
        medicineLabel.setBounds(30,100,300,25);
        frame.add(medicineLabel);

        stockLabel = new JLabel("Stock : -");
        stockLabel.setBounds(30,130,300,25);
        frame.add(stockLabel);

        priceLabel = new JLabel("Price : -");
        priceLabel.setBounds(30,160,300,25);
        frame.add(priceLabel);

        JLabel qtyLbl = new JLabel("Quantity");
        qtyLbl.setBounds(30,200,100,28);
        frame.add(qtyLbl);

        qtyField = new JTextField();
        qtyField.setBounds(120,200,100,28);
        frame.add(qtyField);

        addBtn = new JButton("Add To Bill");
        addBtn.setBounds(250,200,140,28);
        frame.add(addBtn);

        removeBtn = new JButton("Remove Item");
        removeBtn.setBounds(30,250,160,35);
        frame.add(removeBtn);

        clearBtn = new JButton("Clear Bill");
        clearBtn.setBounds(210,250,160,35);
        frame.add(clearBtn);

        String columns[] = {"Medicine", "Price", "Quantity", "Total"};
        model = new DefaultTableModel(columns,0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        billTable = new JTable(model);
        JScrollPane sp = new JScrollPane(billTable);
        sp.setBounds(420,60,530,470);
        frame.add(sp);

        // ================= Dr Fee row (left column, below Clear Bill) =================
        JLabel drFeeLbl = new JLabel("Dr Fee :");
        drFeeLbl.setFont(new Font("Arial", Font.PLAIN, 14));
        drFeeLbl.setBounds(30, 300, 70, 25);
        frame.add(drFeeLbl);

        drFeeField = new JTextField(String.valueOf(appSettingsDAO.getDoctorFee()));
        drFeeField.setBounds(110, 300, 90, 25);
        frame.add(drFeeField);

        JButton noFeeBtn = new JButton("✕");
        noFeeBtn.setBounds(205, 300, 30, 25);
        noFeeBtn.setToolTipText("Remove Dr Fee for this sale");
        noFeeBtn.setMargin(new Insets(0, 0, 0, 0));
        frame.add(noFeeBtn);

        noFeeBtn.addActionListener(e -> {
            drFeeField.setText("0");
            updateGrandTotal();
        });

        drFeeField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateGrandTotal();
            }
        });

        // ================= Tests row (left column) =================
        JLabel testsLbl = new JLabel("Tests :");
        testsLbl.setFont(new Font("Arial", Font.PLAIN, 14));
        testsLbl.setBounds(30, 335, 70, 25);
        frame.add(testsLbl);

        testBox = new JComboBox<>();
        testBox.setBounds(110, 335, 220, 25);
        frame.add(testBox);

        loadAvailableTests();

        testBox.addActionListener(e -> {
            int index = testBox.getSelectedIndex();
            if (index <= 0) return; // placeholder selected, ignore

            TestDAO.TestInfo selectedTest = availableTests.get(index - 1);
            testsModel.addRow(new Object[]{ selectedTest.name, selectedTest.price });
            updateGrandTotal();

            testBox.setSelectedIndex(0); // reset back to placeholder so more tests can be added
        });

        String[] testColumns = {"Test", "Price"};
        testsModel = new DefaultTableModel(testColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1; // cashier can adjust price for this bill only
            }
        };
        testsModel.addTableModelListener(e -> updateGrandTotal());

        testsTable = new JTable(testsModel);
        JScrollPane testsScroll = new JScrollPane(testsTable);
        testsScroll.setBounds(30, 365, 340, 80);
        frame.add(testsScroll);

        removeTestBtn = new JButton("Remove Test");
        removeTestBtn.setBounds(30, 450, 150, 28);
        frame.add(removeTestBtn);

        removeTestBtn.addActionListener(e -> {
            int selectedRow = testsTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(null, "Select a test to remove!");
                return;
            }
            testsModel.removeRow(selectedRow);
            updateGrandTotal();
        });

        // ================= Grand Total (right column, below medicine table) =================
        totalLabel = new JLabel("Grand Total : Rs. 0");
        totalLabel.setFont(new Font("Arial",Font.BOLD,18));
        totalLabel.setBounds(420,545,400,30);
        frame.add(totalLabel);

        removeBtn.addActionListener(e -> {
            int selectedRow = billTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(null, "Select a row from the bill to remove!");
                return;
            }
            model.removeRow(selectedRow);
            updateGrandTotal();
            refreshCurrentStockDisplay();
        });

        clearBtn.addActionListener(e -> {
            if (model.getRowCount() == 0 && testsModel.getRowCount() == 0) {
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(
                    null,
                    "Clear the entire bill?",
                    "Confirm",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                model.setRowCount(0);
                testsModel.setRowCount(0);
                updateGrandTotal();
                refreshCurrentStockDisplay();
            }
        });

        completeSaleBtn = new JButton("Complete Sale");
        completeSaleBtn.setBounds(420,585,180,40);
        frame.add(completeSaleBtn);

        printBtn = new JButton("Print Invoice");
        printBtn.setBounds(630,585,180,40);
        printBtn.setEnabled(false);
        frame.add(printBtn);

        // To share invoiceNo between buttons
        final String[] invoiceNoHolder = new String[1];

        completeSaleBtn.addActionListener(e -> {
            if (calculateGrandTotal() <= 0) {
                JOptionPane.showMessageDialog(null, "Bill is empty!");
                return;
            }

            String invoiceNo = InvoiceService.saveSale(billTable, LoginFrame.loggedInUser, getDrFeeValue(), testsModel);

            if (invoiceNo == null) {
                JOptionPane.showMessageDialog(null, "Sale Failed!");
                return;
            }

            invoiceNoHolder[0] = invoiceNo;

            JOptionPane.showMessageDialog(null, "Sale Completed Successfully!\nInvoice No: " + invoiceNo);

            model.setRowCount(0);
            testsModel.setRowCount(0);
            drFeeField.setText(String.valueOf(appSettingsDAO.getDoctorFee()));
            updateGrandTotal();

            medicineLabel.setText("Medicine : -");
            stockLabel.setText("Stock : -");
            priceLabel.setText("Price : -");

            HomeFrame.refreshDashboardStatic();

            printBtn.setEnabled(true);
        });

        printBtn.addActionListener(e -> {
            if (invoiceNoHolder[0] != null) {
                InvoicePrinter.printInvoice(invoiceNoHolder[0]);
            } else {
                JOptionPane.showMessageDialog(null, "No invoice to print!");
            }
        });

        searchBtn.addActionListener(e -> {
            SalesDAO.MedicineInfo info = salesDAO.findMedicineByName(searchField.getText());

            if (info != null) {
                medicineLabel.setText("Medicine : " + info.name);
                int reserved = getQuantityInBillFor(info.name);
                int available = info.quantity - reserved;
                stockLabel.setText("Stock : " + available);
                priceLabel.setText("Price : Rs. " + info.salePrice);
            } else {
                JOptionPane.showMessageDialog(null, "Medicine Not Found!");
            }
        });

        addBtn.addActionListener(e -> {

            try {

                String medicine =
                        medicineLabel.getText().replace("Medicine : ", "");

                if (medicine.equals("-")) {

                    JOptionPane.showMessageDialog(
                            null,
                            "Search a medicine first!"
                    );

                    return;

                }

                double price =
                        Double.parseDouble(
                                priceLabel.getText()
                                        .replace("Price : Rs. ", "")
                        );

                int qty =
                        Integer.parseInt(qtyField.getText());

                // This already reflects DB stock minus whatever's already in the
                // bill for this medicine (computed at search time) - so it's the
                // remaining capacity, not the raw DB quantity.
                int availableStock =
                Integer.parseInt(
                        stockLabel.getText()
                                .replace("Stock : ", "")
                );

        if (qty <= 0) {

            JOptionPane.showMessageDialog(
                    null,
                    "Quantity must be greater than 0!"
            );

            return;
        }

        if (qty > availableStock) {

            JOptionPane.showMessageDialog(
                    null,
                    "Not enough stock available! Only " + availableStock + " remaining."
            );

            return;
        }

                boolean found = false;

                for (int i = 0; i < model.getRowCount(); i++) {

                    String med =
                            model.getValueAt(i, 0).toString();

                    if (med.equalsIgnoreCase(medicine)) {

                        int oldQty =
                                Integer.parseInt(
                                        model.getValueAt(i, 2).toString()
                                );

                        int newQty = oldQty + qty;

                        model.setValueAt(newQty, i, 2);

                        model.setValueAt(price * newQty, i, 3);

                        found = true;

                        break;

                    }

                }

                if (!found) {

                    model.addRow(new Object[]{

                            medicine,
                            price,
                            qty,
                            price * qty

                    });

                }

                updateGrandTotal();

                qtyField.setText("");

        searchField.setText("");
        suggestionPopup.setVisible(false);

        medicineLabel.setText("Medicine : -");
        stockLabel.setText("Stock : -");
        priceLabel.setText("Price : -");

        searchField.requestFocus();

            }

            catch (Exception ex) {

                JOptionPane.showMessageDialog(
                        null,
                        "Invalid Quantity!"
                );

            }

        });
        qtyField.addActionListener(e -> {

            addBtn.doClick();

        });

        updateGrandTotal();

        frame.setVisible(true);
    }

    private void loadAvailableTests() {
        availableTests = testDAO.getAllTests();
        testBox.removeAllItems();
        testBox.addItem("-- Select Test --");
        for (TestDAO.TestInfo t : availableTests) {
            testBox.addItem(t.name + " (Rs. " + t.price + ")");
        }
    }

    /** How much of this medicine is already sitting in the current bill (0 if none). */
    private int getQuantityInBillFor(String medicineName) {
        for (int i = 0; i < model.getRowCount(); i++) {
            String med = model.getValueAt(i, 0).toString();
            if (med.equalsIgnoreCase(medicineName)) {
                return Integer.parseInt(model.getValueAt(i, 2).toString());
            }
        }
        return 0;
    }

    /** Re-fetches DB stock and re-subtracts what's reserved in the bill for whichever
     *  medicine is currently displayed, so the Stock label stays accurate as the bill changes. */
    private void refreshCurrentStockDisplay() {
        String medText = medicineLabel.getText().replace("Medicine : ", "");
        if (medText.equals("-")) return;

        SalesDAO.MedicineInfo info = salesDAO.findMedicineByName(medText);
        if (info != null) {
            int reserved = getQuantityInBillFor(info.name);
            int available = info.quantity - reserved;
            stockLabel.setText("Stock : " + available);
        }
    }

    private void setupSuggestionPopup() {
        suggestionListModel = new DefaultListModel<>();
        suggestionList = new JList<>(suggestionListModel);
        suggestionList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane listScroll = new JScrollPane(suggestionList);
        listScroll.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        suggestionPopup = new JWindow(frame);
        suggestionPopup.setFocusableWindowState(false); // keep keyboard focus on the search field
        suggestionPopup.setLayout(new BorderLayout());
        suggestionPopup.add(listScroll, BorderLayout.CENTER);

        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = suggestionList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    suggestionList.setSelectedIndex(index);
                    commitSelectedSuggestion();
                    searchBtn.doClick();
                    qtyField.requestFocus();
                }
            }
        });

        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // Small delay so a click on the suggestion list still registers first
                Timer timer = new Timer(150, ev -> suggestionPopup.setVisible(false));
                timer.setRepeats(false);
                timer.start();
            }
        });
    }

    private void commitSelectedSuggestion() {
        String chosen = suggestionList.getSelectedValue();
        if (chosen != null) {
            searchField.setText(chosen);
        }
        suggestionPopup.setVisible(false);
    }

    private void showSuggestions() {
        String text = searchField.getText().trim();

        if (text.isEmpty()) {
            suggestionPopup.setVisible(false);
            return;
        }

        List<String> suggestions = salesDAO.suggestMedicineNames(text);

        if (suggestions.isEmpty()) {
            suggestionPopup.setVisible(false);
            return;
        }

        suggestionListModel.clear();
        for (String name : suggestions) {
            suggestionListModel.addElement(name);
        }
        suggestionList.setSelectedIndex(0);

        if (searchField.isShowing()) {
            Point location = searchField.getLocationOnScreen();
            suggestionPopup.setLocation(location.x, location.y + searchField.getHeight());
            int height = Math.min(160, suggestions.size() * 22 + 10);
            suggestionPopup.setSize(searchField.getWidth(), height);
            suggestionPopup.setVisible(true);
            suggestionPopup.toFront();
        }
    }

    private double calculateGrandTotal() {

        double grandTotal = 0;

        for (int i = 0; i < model.getRowCount(); i++) {

            grandTotal += Double.parseDouble(
                    model.getValueAt(i, 3).toString()
            );

        }

        grandTotal += getDrFeeValue();

        for (int i = 0; i < testsModel.getRowCount(); i++) {
            try {
                grandTotal += Double.parseDouble(testsModel.getValueAt(i, 1).toString());
            } catch (Exception ignored) {
                // Ignore a row mid-edit with an invalid/empty price
            }
        }

        return grandTotal;
    }

    private void updateGrandTotal() {

        double grandTotal = calculateGrandTotal();

        totalLabel.setText("Grand Total : Rs. " + grandTotal);

        // Sale is allowed whenever there's an amount to charge - medicines,
        // Dr Fee, Test Fees, or any combination of the three.
        boolean hasAmount = grandTotal > 0;
        completeSaleBtn.setEnabled(hasAmount);
        printBtn.setEnabled(hasAmount);

    }

    private double getDrFeeValue() {
        try {
            return Double.parseDouble(drFeeField.getText().trim());
        } catch (Exception ex) {
            return 0;
        }
    }
}