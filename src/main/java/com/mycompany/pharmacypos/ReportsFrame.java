package com.mycompany.pharmacypos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Reports screen, opened from the Home dashboard.
 *
 * Built entirely with layout managers (no absolute positioning) so it
 * resizes and maximizes cleanly like any standard desktop app screen.
 *
 * To add a new report in the future: add one more registerReport(...) call
 * inside buildReportCatalog() - it will automatically appear in its category
 * in the left-hand list, use the shared date filter bar, and render through
 * the shared table/summary area. Nothing else needs to change.
 */
public class ReportsFrame {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    /** Name of the report that gets the extra Search box (Medicine / Company /
     *  Distributor / Batch No), matching the name it's registered under below. */
    private static final String NEWLY_ADDED_STOCK_REPORT = "Newly Added Stock Report";

    /** Columns eligible for the search box on that report - see the column
     *  array passed to its ReportDefinition in buildReportCatalog(). */
    private static final int[] STOCK_SEARCH_COLUMNS = {0, 1, 2, 3}; // Medicine, Company, Distributor, Batch No

    private final ReportsDAO reportsDAO = new ReportsDAO();

    private JFrame frame;
    private JLabel reportTitleLabel;
    private JLabel statusLabel;
    private JPanel summaryPanel;
    private DefaultTableModel tableModel;
    private JTable resultsTable;
    private TableRowSorter<DefaultTableModel> tableSorter;
    private JPanel searchPanel;
    private JTextField searchField;
    private JComboBox<String> dateFilterBox;
    private JPanel customRangePanel;
    private JTextField customStartField;
    private JTextField customEndField;
    private JPanel specificDatePanel;
    private JTextField specificDateField;

    private ReportDefinition currentReport;
    private JButton currentActiveButton;

    private final Map<String, List<ReportDefinition>> catalog = new LinkedHashMap<>();


    public void show() {
        buildReportCatalog();

        frame = new JFrame("Reports");
        frame.setSize(1150, 700);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setMinimumSize(new Dimension(850, 500));

        frame.add(buildReportListPanel(), BorderLayout.WEST);
        frame.add(buildMainPanel(), BorderLayout.CENTER);

        // Open full-screen by default, matching Medicines, Sales, Sales History,
        // Low Stock, Expiry Alert, and Settings.
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);
    }

    // ============================================================
    //  REPORT CATALOG - add new reports here
    // ============================================================

    private void buildReportCatalog() {
        List<ReportDefinition> revenue = new java.util.ArrayList<>();
        List<ReportDefinition> medicine = new java.util.ArrayList<>();
        List<ReportDefinition> inventory = new java.util.ArrayList<>();
        catalog.put("Revenue Reports", revenue);
        catalog.put("Medicine Reports", medicine);
        catalog.put("Inventory Reports", inventory);

        revenue.add(new ReportDefinition("Revenue Report", (s, e) -> {
            Object[] b = reportsDAO.revenueBreakdown(s, e);
            return new ReportResult(new String[]{"Medicine Fee", "Test Fee", "Doctor Fee", "Total Revenue"},
                    java.util.Collections.singletonList(b),
                    "Combined revenue for the selected period.");
        }));

        revenue.add(new ReportDefinition("Sales Summary", (s, e) -> {
            Object[] sm = reportsDAO.salesSummary(s, e);
            return new ReportResult(new String[]{"Invoices", "Total Revenue", "Avg. Revenue / Invoice"},
                    java.util.Collections.singletonList(new Object[]{ sm[0], currency((double) sm[1]), currency((double) sm[2]) }));
        }));

        medicine.add(new ReportDefinition(NEWLY_ADDED_STOCK_REPORT, (s, e) -> {
            List<Object[]> rows = reportsDAO.newlyAddedStock(s, e);
            List<Object[]> formattedRows = new java.util.ArrayList<>();
            for (Object[] r : rows) {
                formattedRows.add(new Object[]{
                    r[0], r[1], r[2], r[3], currency((double) r[4]), r[5], r[6]
                });
            }
            return new ReportResult(
                    new String[]{"Medicine", "Company", "Distributor", "Batch No", "Purchase Price", "Quantity Added", "Added On"},
                    formattedRows);
        }));

        medicine.add(new ReportDefinition("Medicine Sales Report", (s, e) -> {
            List<Object[]> rows = reportsDAO.medicineSalesReport(s, e);
            Object[] summary = reportsDAO.medicineSalesSummary(rows);

            List<Object[]> formattedRows = new java.util.ArrayList<>();
            for (Object[] r : rows) {
                formattedRows.add(new Object[]{
                    r[0], r[1], r[2], r[3], r[4],
                    currency((double) r[5]), currency((double) r[6]), currency((double) r[7]), currency((double) r[8]),
                    r[9], r[10], r[11]
                });
            }

            Map<String, String> summaryStats = new LinkedHashMap<>();
            summaryStats.put("Total Medicines Sold", String.valueOf(summary[0]));
            summaryStats.put("Total Quantity Sold", String.valueOf(summary[1]));
            summaryStats.put("Total Medicine Revenue", currency((double) summary[2]));
            summaryStats.put("Total Medicine Profit", currency((double) summary[3]));

            return new ReportResult(
                    new String[]{"Invoice No", "Sale Date & Time", "Medicine Name", "Company", "Qty Sold",
                        "Purchase Price", "Sale Price", "Profit/Unit", "Total Profit", "Expiry Date", "Batch No", "Distributor"},
                    formattedRows,
                    "Complete sales history for every medicine sold in the selected period.",
                    summaryStats);
        }));

        inventory.add(new ReportDefinition("Inventory Adjustment History", (s, e) ->
            new ReportResult(new String[]{"Medicine", "Previous Qty", "New Qty", "Change", "Adjusted On", "Updated By"},
                    reportsDAO.inventoryAdjustments(s, e),
                    "Full audit trail of manual quantity edits made on the Medicine Management screen.")));
    }

    private static String currency(double amount) {
        return "Rs. " + String.format("%.2f", amount);
    }

    // ============================================================
    //  LEFT PANEL - report list
    // ============================================================

    private JComponent buildReportListPanel() {
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);
        listPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        for (Map.Entry<String, List<ReportDefinition>> entry : catalog.entrySet()) {
            JLabel categoryLbl = new JLabel(entry.getKey());
            categoryLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
            categoryLbl.setForeground(new Color(0, 102, 102));
            categoryLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            categoryLbl.setBorder(BorderFactory.createEmptyBorder(10, 4, 8, 4));
            listPanel.add(categoryLbl);

            for (ReportDefinition def : entry.getValue()) {
                JButton btn = new JButton("<html><body style='width:190px'>" + def.name + "</body></html>");
                btn.setHorizontalAlignment(SwingConstants.LEFT);
                btn.setAlignmentX(Component.LEFT_ALIGNMENT);
                btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, btn.getPreferredSize().height + 12));
                btn.setFocusPainted(false);
                btn.setBorderPainted(false);
                btn.setContentAreaFilled(true);
                btn.setBackground(Color.WHITE);
                btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btn.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

                btn.addActionListener(e -> selectReport(def, btn));
                listPanel.add(btn);
            }
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setPreferredSize(new Dimension(270, 0));
        scroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(225, 225, 225)));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private void selectReport(ReportDefinition def, JButton sourceButton) {
        if (currentActiveButton != null) {
            currentActiveButton.setBackground(Color.WHITE);
            currentActiveButton.setForeground(Color.BLACK);
        }
        sourceButton.setBackground(new Color(0, 150, 136));
        sourceButton.setForeground(Color.WHITE);
        currentActiveButton = sourceButton;

        currentReport = def;
        reportTitleLabel.setText(def.name);

        boolean showSearch = NEWLY_ADDED_STOCK_REPORT.equals(def.name);
        searchField.setText("");
        searchPanel.setVisible(showSearch);
        tableSorter.setRowFilter(null);

        generateReport();
    }

    // ============================================================
    //  MAIN PANEL - filter bar + results
    // ============================================================

    private JComponent buildMainPanel() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(new Color(245, 248, 250));

        main.add(buildFilterBar(), BorderLayout.NORTH);
        main.add(buildResultsArea(), BorderLayout.CENTER);

        return main;
    }

    private JComponent buildFilterBar() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(225, 225, 225)),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)));

        reportTitleLabel = new JLabel("Select a report from the left");
        reportTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        reportTitleLabel.setForeground(new Color(30, 30, 30));
        reportTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(reportTitleLabel);
        container.add(Box.createVerticalStrut(10));

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterRow.setBackground(Color.WHITE);
        filterRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        filterRow.add(new JLabel("Date Range:"));

        String[] filters = {
            "Today", "Yesterday", "This Week", "Last Week", "This Month", "Last Month",
            "Specific Date", "Date Range (From - To)"
        };
        dateFilterBox = new JComboBox<>(filters);
        filterRow.add(dateFilterBox);

        customRangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        customRangePanel.setBackground(Color.WHITE);
        customRangePanel.add(new JLabel("From (YYYY-MM-DD):"));
        customStartField = new JTextField(LocalDate.now().minusDays(6).format(ISO), 10);
        customRangePanel.add(customStartField);
        customRangePanel.add(new JLabel("To:"));
        customEndField = new JTextField(LocalDate.now().format(ISO), 10);
        customRangePanel.add(customEndField);
        customRangePanel.setVisible(false);
        filterRow.add(customRangePanel);

        specificDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        specificDatePanel.setBackground(Color.WHITE);
        specificDatePanel.add(new JLabel("Date (YYYY-MM-DD):"));
        specificDateField = new JTextField(LocalDate.now().format(ISO), 10);
        specificDatePanel.add(specificDateField);
        specificDatePanel.setVisible(false);
        filterRow.add(specificDatePanel);

        dateFilterBox.addActionListener(e -> {
            String sel = (String) dateFilterBox.getSelectedItem();
            customRangePanel.setVisible("Date Range (From - To)".equals(sel));
            specificDatePanel.setVisible("Specific Date".equals(sel));
        });

        JButton generateBtn = new JButton("Generate");
        generateBtn.setBackground(new Color(0, 150, 136));
        generateBtn.setForeground(Color.WHITE);
        generateBtn.setFocusPainted(false);
        generateBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        generateBtn.addActionListener(e -> generateReport());
        filterRow.add(generateBtn);

        container.add(filterRow);

        searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchPanel.setBackground(Color.WHITE);
        searchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        searchPanel.setVisible(false);

        JLabel searchLbl = new JLabel("Search:");
        searchPanel.add(searchLbl);

        searchField = new JTextField(24);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.putClientProperty("JTextField.placeholderText", "Medicine, Company, Distributor, or Batch No...");
        searchPanel.add(searchField);
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { applySearchFilter(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { applySearchFilter(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { applySearchFilter(); }
        });

        container.add(searchPanel);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(new Color(180, 60, 60));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        container.add(Box.createVerticalStrut(6));
        container.add(statusLabel);

        return container;
    }

    private JComponent buildResultsArea() {
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBackground(new Color(245, 248, 250));
        resultsPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        summaryPanel = new JPanel();
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.Y_AXIS));
        summaryPanel.setBackground(new Color(245, 248, 250));
        resultsPanel.add(summaryPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultsTable = new JTable(tableModel);
        tableSorter = new TableRowSorter<>(tableModel);
        resultsTable.setRowSorter(tableSorter);
        resultsTable.setRowHeight(26);
        resultsTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        resultsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JScrollPane tableScroll = new JScrollPane(resultsTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(225, 225, 225)));
        resultsPanel.add(tableScroll, BorderLayout.CENTER);

        return resultsPanel;
    }

    // ============================================================
    //  GENERATE
    // ============================================================

    private void generateReport() {
        if (currentReport == null) {
            return;
        }
        statusLabel.setText(" ");

        LocalDate[] range = resolveDateRange();
        if (range == null) {
            return; // resolveDateRange already put an error in statusLabel
        }

        try {
            ReportResult result = currentReport.runner.run(range[0], range[1]);
            renderResult(result);
        } catch (IllegalStateException schemaIssue) {
            // Expected/explained condition (e.g. missing DB column) - show as-is, not a stack trace.
            tableModel.setDataVector(new Object[][]{}, new Object[]{});
            summaryPanel.removeAll();
            summaryPanel.revalidate();
            summaryPanel.repaint();
            statusLabel.setText(schemaIssue.getMessage().split("\n")[0]);
            JOptionPane.showMessageDialog(frame, schemaIssue.getMessage(), "Report Unavailable", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("Error generating report: " + ex.getMessage());
        }
    }

    private LocalDate[] resolveDateRange() {
        LocalDate today = LocalDate.now();
        String selected = (String) dateFilterBox.getSelectedItem();
        if (selected == null) selected = "Today";

        switch (selected) {
            case "Today":
                return new LocalDate[]{ today, today };
            case "Yesterday":
                LocalDate yesterday = today.minusDays(1);
                return new LocalDate[]{ yesterday, yesterday };
            case "This Week": {
                LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
                return new LocalDate[]{ startOfWeek, today };
            }
            case "Last Week": {
                LocalDate startOfThisWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
                LocalDate startOfLastWeek = startOfThisWeek.minusWeeks(1);
                LocalDate endOfLastWeek = startOfThisWeek.minusDays(1);
                return new LocalDate[]{ startOfLastWeek, endOfLastWeek };
            }
            case "This Month":
                return new LocalDate[]{ today.withDayOfMonth(1), today };
            case "Last Month": {
                LocalDate firstOfThisMonth = today.withDayOfMonth(1);
                LocalDate lastMonthEnd = firstOfThisMonth.minusDays(1);
                LocalDate lastMonthStart = lastMonthEnd.withDayOfMonth(1);
                return new LocalDate[]{ lastMonthStart, lastMonthEnd };
            }
            case "Specific Date":
                try {
                    LocalDate date = LocalDate.parse(specificDateField.getText().trim(), ISO);
                    return new LocalDate[]{ date, date };
                } catch (DateTimeParseException ex) {
                    statusLabel.setText("Enter a valid date in YYYY-MM-DD format.");
                    return null;
                }
            case "Date Range (From - To)":
                try {
                    LocalDate start = LocalDate.parse(customStartField.getText().trim(), ISO);
                    LocalDate end = LocalDate.parse(customEndField.getText().trim(), ISO);
                    if (start.isAfter(end)) {
                        statusLabel.setText("\"From\" date must not be after \"To\" date.");
                        return null;
                    }
                    return new LocalDate[]{ start, end };
                } catch (DateTimeParseException ex) {
                    statusLabel.setText("Enter valid dates in YYYY-MM-DD format.");
                    return null;
                }
            default:
                return new LocalDate[]{ today, today };
        }
    }

    private void renderResult(ReportResult result) {
        summaryPanel.removeAll();
        if (result.note != null) {
            JLabel noteLbl = new JLabel(result.note);
            noteLbl.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            noteLbl.setForeground(Color.GRAY);
            noteLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
            summaryPanel.add(noteLbl);
        }
        if (result.summaryStats != null && !result.summaryStats.isEmpty()) {
            summaryPanel.add(buildSummaryStatsRow(result.summaryStats));
            summaryPanel.add(Box.createVerticalStrut(10));
        }
        summaryPanel.revalidate();
        summaryPanel.repaint();

        Object[][] rowsArray = result.rows.toArray(new Object[0][]);
        tableModel.setDataVector(rowsArray, result.columns);

        if (result.rows.isEmpty()) {
            statusLabel.setText("No data found for the selected range.");
            statusLabel.setForeground(new Color(120, 120, 120));
        } else {
            statusLabel.setText(" ");
        }

        // Re-apply any active search keyword on top of the freshly generated,
        // date-filtered rows (date range filters first, search narrows further).
        applySearchFilter();
    }

    /** Filters the currently displayed rows by the Search box, restricted to
     *  the Newly Added Stock Report (Medicine / Company / Distributor /
     *  Batch No columns). No-op - and clears any filter - for every other
     *  report, since only this one shows the search box. */
    private void applySearchFilter() {
        if (currentReport == null || !NEWLY_ADDED_STOCK_REPORT.equals(currentReport.name)) {
            tableSorter.setRowFilter(null);
            return;
        }

        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            tableSorter.setRowFilter(null);
            return;
        }

        String lower = text.toLowerCase();
        tableSorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                for (int col : STOCK_SEARCH_COLUMNS) {
                    if (col >= entry.getModel().getColumnCount()) continue;
                    Object value = entry.getValue(col);
                    if (value != null && value.toString().toLowerCase().contains(lower)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    /** A row of small "stat cards" (label + value) for a report's Summary section,
     *  shown above the results table - e.g. Total Medicines Sold / Total Quantity
     *  Sold / Total Medicine Revenue / Total Medicine Profit for the selected
     *  date range on the Medicine Sales Report. */
    private JComponent buildSummaryStatsRow(Map<String, String> stats) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row.setBackground(new Color(245, 248, 250));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder());

        for (Map.Entry<String, String> stat : stats.entrySet()) {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(225, 225, 225)),
                    BorderFactory.createEmptyBorder(10, 16, 10, 16)));

            JLabel valueLbl = new JLabel(stat.getValue());
            valueLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
            valueLbl.setForeground(new Color(0, 102, 102));
            valueLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel labelLbl = new JLabel(stat.getKey());
            labelLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            labelLbl.setForeground(new Color(110, 110, 110));
            labelLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

            card.add(valueLbl);
            card.add(labelLbl);
            row.add(card);
        }

        return row;
    }

    // ============================================================
    //  Small data-holder types
    // ============================================================

    @FunctionalInterface
    private interface ReportRunner {
        ReportResult run(LocalDate start, LocalDate end) throws Exception;
    }

    private static class ReportDefinition {
        final String name;
        final ReportRunner runner;

        ReportDefinition(String name, ReportRunner runner) {
            this.name = name;
            this.runner = runner;
        }
    }

    private static class ReportResult {
        final String[] columns;
        final List<Object[]> rows;
        final String note;
        final Map<String, String> summaryStats;

        ReportResult(String[] columns, List<Object[]> rows) {
            this(columns, rows, null);
        }

        ReportResult(String[] columns, List<Object[]> rows, String note) {
            this(columns, rows, note, null);
        }

        ReportResult(String[] columns, List<Object[]> rows, String note, Map<String, String> summaryStats) {
            this.columns = columns;
            this.rows = rows;
            this.note = note;
            this.summaryStats = summaryStats;
        }
    }
}
