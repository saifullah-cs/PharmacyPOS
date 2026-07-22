package com.mycompany.pharmacypos;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

public class HomeFrame {

    private static HomeFrame instance;

    // ================= Window / Layout =================
    private JFrame frame;
    private JPanel contentPanel;
    private JPanel sidebar;

    // ================= Dashboard Cards =================
    private JLabel revenueValue, invoicesValue, medicineFeeValue, doctorFeeValue, testFeeValue;
    private JLabel versionLabel;

    // ================= Alerts =================
    private DefaultListModel<String> lowStockListModel;
    private DefaultListModel<String> expiringListModel;
    private JList<String> lowStockList;
    private JList<String> expiringList;
    private Timer lowStockMarqueeTimer;
    private Timer expiringMarqueeTimer;

    // ================= Recent Activity =================
    private static final int RECENT_ACTIVITY_ROWS = 3;
    private DefaultTableModel recentActivityModel;

    // ================= Sidebar Buttons =================
    private JButton medicineBtn;
    private JButton lowStockBtn;
    private JButton expiryBtn;
    private JButton salesBtn;
    private JButton historyBtn;
    private JButton reportsBtn;
    private JButton settingsBtn;
    private JButton logoutBtn;

    private String role;

    // ================= Data Access =================
    private final DashboardDAO dashboardDAO = new DashboardDAO();
    private final SalesDAO salesDAO = new SalesDAO();

    public HomeFrame(String role) {
        instance = this;
        this.role = role;
        initializeUI();
    }

    // ============================================================
    //  WINDOW SETUP
    // ============================================================

    private void initializeUI() {
        frame = new JFrame("Health Haven POS");
        frame.setSize(1200, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);
        frame.setLocationRelativeTo(null);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setMinimumSize(new Dimension(900, 600));

        createSidebar();
        createContentArea();
        attachButtonActions();

        frame.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                resizeMainAreas();
            }
        });

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                stopMarquees();
            }
        });

        frame.setVisible(true);
        resizeMainAreas();
    }

    private void resizeMainAreas() {
        int w = frame.getContentPane().getWidth();
        int h = frame.getContentPane().getHeight();
        sidebar.setBounds(0, 0, 250, h);
        contentPanel.setBounds(250, 0, Math.max(0, w - 250), h);
        if (versionLabel != null) {
            versionLabel.setBounds(20, Math.max(620, h - 30), 210, 18);
        }
        frame.revalidate();
        frame.repaint();
    }

    // ============================================================
    //  SIDEBAR
    // ============================================================

    private void createSidebar() {
        sidebar = new JPanel();
        sidebar.setLayout(null);
        sidebar.setBounds(0, 0, 250, 700);
        sidebar.setBackground(new Color(11, 40, 55));
        frame.add(sidebar);

        JLabel clinicName = new JLabel("🏥 HEALTH HAVEN");
        clinicName.setForeground(Color.WHITE);
        clinicName.setFont(new Font("Segoe UI", Font.BOLD, 24));
        clinicName.setBounds(20, 25, 220, 35);
        sidebar.add(clinicName);

        JLabel subTitle = new JLabel("Clinic Pharmacy");
        subTitle.setForeground(new Color(220, 220, 220));
        subTitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subTitle.setBounds(22, 58, 180, 20);
        sidebar.add(subTitle);

        JSeparator separator = new JSeparator();
        separator.setBounds(20, 90, 200, 3);
        sidebar.add(separator);

        medicineBtn = createMenuButton("Medicines", 110);
        lowStockBtn = createMenuButton("Low Stock", 160);
        expiryBtn = createMenuButton("Expiry Alert", 210);
        salesBtn = createMenuButton("Sales", 260);
        historyBtn = createMenuButton("Sales History", 310);
        reportsBtn = createMenuButton("Reports", 360);
        settingsBtn = createMenuButton("Settings", 410);
        logoutBtn = createMenuButton("Logout", 510);

        versionLabel = new JLabel("Version 1.0");
        versionLabel.setForeground(new Color(150, 165, 175));
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        versionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        versionLabel.setBounds(20, 670, 210, 18);
        sidebar.add(versionLabel);

        sidebar.add(medicineBtn);
        sidebar.add(lowStockBtn);
        sidebar.add(expiryBtn);
        sidebar.add(salesBtn);
        sidebar.add(historyBtn);
        sidebar.add(reportsBtn);
        sidebar.add(settingsBtn);
        sidebar.add(logoutBtn);

        styleButton(medicineBtn);
        styleButton(lowStockBtn);
        styleButton(expiryBtn);
        styleButton(salesBtn);
        styleButton(historyBtn);
        styleButton(reportsBtn);
        styleButton(settingsBtn);
        styleButton(logoutBtn);

        // Settings is admin-only for now (manages user passwords)
        boolean isAdmin = "admin".equalsIgnoreCase(role);
        settingsBtn.setVisible(isAdmin);
    }

    private JButton createMenuButton(String text, int y) {
        JButton btn = new JButton(text);
        btn.setBounds(18, y, 214, 44);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMargin(new Insets(0, 18, 0, 0));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(true);
        btn.setBackground(new Color(22, 78, 99));
        btn.setForeground(Color.WHITE);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(34, 120, 150));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(22, 78, 99));
            }
        });

        return btn;
    }

    private void styleButton(JButton btn) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(22, 78, 99));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(0, 18, 0, 0));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(30, 110, 140));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(22, 78, 99));
            }
        });
    }

    private void setActiveButton(JButton activeBtn) {
        JButton[] buttons = { medicineBtn, lowStockBtn, salesBtn, historyBtn, reportsBtn, settingsBtn, logoutBtn };

        for (JButton btn : buttons) {
            btn.setBackground(new Color(22, 78, 99));
        }

        activeBtn.setBackground(new Color(0, 150, 136));
    }

    // ============================================================
    //  CONTENT AREA / DASHBOARD
    // ============================================================

    private void createContentArea() {
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBounds(250, 0, 950, 700);
        contentPanel.setBackground(new Color(245, 248, 250));

        // ---- Header (fixed at top) ----
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(new Color(245, 248, 250));
        headerPanel.setBorder(new EmptyBorder(18, 40, 6, 40));

        JLabel heading = new JLabel("HEALTH HAVEN CLINIC");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 30));
        heading.setForeground(new Color(0, 102, 102));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(heading);

        JLabel subHeading = new JLabel("Pharmacy Management System");
        subHeading.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        subHeading.setForeground(Color.GRAY);
        subHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(subHeading);

        JLabel welcome = new JLabel("Welcome, " + LoginFrame.loggedInUser);
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 16));
        welcome.setForeground(new Color(60, 60, 60));
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);
        welcome.setBorder(new EmptyBorder(8, 0, 0, 0));
        headerPanel.add(welcome);

        contentPanel.add(headerPanel, BorderLayout.NORTH);

        // ---- Body: cards, alerts, recent activity (fixed - no scrolling) ----
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(new Color(245, 248, 250));
        body.setBorder(new EmptyBorder(5, 40, 15, 40));

        body.add(buildDashboardCards());
        body.add(Box.createVerticalStrut(16));
        body.add(buildAlertsRow());
        body.add(Box.createVerticalStrut(10));
        body.add(buildRecentActivityPanel());

        contentPanel.add(body, BorderLayout.CENTER);

        frame.add(contentPanel);

        loadDashboardStats();
        startMarquees();
    }

    // ============================================================
    //  SUMMARY CARDS (Today's Revenue / Invoices / Fee breakdown)
    // ============================================================

    private JComponent buildDashboardCards() {
        JPanel cardsRow = new JPanel(new GridLayout(1, 5, 15, 0));
        cardsRow.setBackground(new Color(245, 248, 250));
        cardsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        cardsRow.setPreferredSize(new Dimension(10, 130));

        JPanel card1 = createStatCard("Today's Revenue", new Color(0, 150, 136));
        revenueValue = (JLabel) card1.getClientProperty("valueLabel");

        JPanel card2 = createStatCard("Today's Invoices", new Color(33, 150, 243));
        invoicesValue = (JLabel) card2.getClientProperty("valueLabel");

        JPanel card3 = createStatCard("Medicine Fee Today", new Color(156, 39, 176));
        medicineFeeValue = (JLabel) card3.getClientProperty("valueLabel");

        JPanel card4 = createStatCard("Doctor Fee Today", new Color(255, 152, 0));
        doctorFeeValue = (JLabel) card4.getClientProperty("valueLabel");

        JPanel card5 = createStatCard("Test Fee Today", new Color(233, 30, 99));
        testFeeValue = (JLabel) card5.getClientProperty("valueLabel");

        cardsRow.add(card1);
        cardsRow.add(card2);
        cardsRow.add(card3);
        cardsRow.add(card4);
        cardsRow.add(card5);

        return cardsRow;
    }

    /** Builds one modern stat card (accent stripe + title + big value) and stashes
     *  its value JLabel as a client property so the caller can grab and update it. */
    private JPanel createStatCard(String title, Color accent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(228, 228, 228)),
                new EmptyBorder(14, 16, 14, 12)));

        JPanel accentBar = new JPanel();
        accentBar.setBackground(accent);
        accentBar.setPreferredSize(new Dimension(5, 10));
        card.add(accentBar, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(Color.WHITE);
        textPanel.setBorder(new EmptyBorder(0, 12, 0, 0));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        titleLbl.setForeground(new Color(110, 110, 110));
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueLbl = new JLabel("—");
        valueLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLbl.setForeground(new Color(35, 35, 35));
        valueLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLbl.setBorder(new EmptyBorder(8, 0, 0, 0));

        textPanel.add(titleLbl);
        textPanel.add(valueLbl);
        card.add(textPanel, BorderLayout.CENTER);

        card.putClientProperty("valueLabel", valueLbl);

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBackground(new Color(250, 253, 253));
                textPanel.setBackground(new Color(250, 253, 253));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBackground(Color.WHITE);
                textPanel.setBackground(Color.WHITE);
            }
        });

        return card;
    }

    // ============================================================
    //  ALERTS - Low Stock / Expiring Medicines (auto-scrolling lists)
    // ============================================================

    private JComponent buildAlertsRow() {
        JPanel alertsRow = new JPanel(new GridLayout(1, 2, 15, 0));
        alertsRow.setBackground(new Color(245, 248, 250));
        alertsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        alertsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        alertsRow.setPreferredSize(new Dimension(10, 140));

        lowStockListModel = new DefaultListModel<>();
        lowStockList = new JList<>(lowStockListModel);
        alertsRow.add(buildAlertPanel("⚠ Low Stock Medicines", new Color(230, 81, 0), lowStockList));

        expiringListModel = new DefaultListModel<>();
        expiringList = new JList<>(expiringListModel);
        alertsRow.add(buildAlertPanel("⏳ Expiring Medicines", new Color(198, 40, 40), expiringList));

        return alertsRow;
    }

    private JPanel buildAlertPanel(String title, Color accent, JList<String> list) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(228, 228, 228)),
                new EmptyBorder(12, 14, 12, 14)));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLbl.setForeground(accent);
        titleLbl.setBorder(new EmptyBorder(0, 0, 8, 0));
        panel.add(titleLbl, BorderLayout.NORTH);

        list.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        list.setFixedCellHeight(24);
        list.setBackground(Color.WHITE);
        list.setSelectionBackground(new Color(245, 245, 245));
        list.setSelectionForeground(Color.BLACK);
        list.setFocusable(false);

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    /** Gently auto-scrolls each alert list one row at a time (pausing on each row),
     *  so more entries than fit on screen are still visible without a scrollbar -
     *  a subtle, non-distracting way to surface everything that needs attention. */
    private void startMarquees() {
        lowStockMarqueeTimer = new Timer(2200, e -> advanceMarquee(lowStockList, lowStockListModel));
        lowStockMarqueeTimer.start();

        expiringMarqueeTimer = new Timer(2600, e -> advanceMarquee(expiringList, expiringListModel));
        expiringMarqueeTimer.start();
    }

    private void stopMarquees() {
        if (lowStockMarqueeTimer != null) lowStockMarqueeTimer.stop();
        if (expiringMarqueeTimer != null) expiringMarqueeTimer.stop();
    }

    private int lowStockMarqueeIndex = 0;
    private int expiringMarqueeIndex = 0;

    private void advanceMarquee(JList<String> list, DefaultListModel<String> model) {
        if (model.isEmpty()) return;
        boolean isLowStock = (list == lowStockList);
        int index = isLowStock ? lowStockMarqueeIndex : expiringMarqueeIndex;
        index = (index + 1) % model.getSize();
        list.ensureIndexIsVisible(index);
        if (isLowStock) {
            lowStockMarqueeIndex = index;
        } else {
            expiringMarqueeIndex = index;
        }
    }

    // ============================================================
    //  RECENT ACTIVITY (latest completed sales)
    // ============================================================

    private JComponent buildRecentActivityPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(228, 228, 228)),
                new EmptyBorder(14, 16, 14, 16)));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        panel.setPreferredSize(new Dimension(10, 170));

        JLabel titleLbl = new JLabel("Recent Activity");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLbl.setForeground(new Color(0, 102, 102));
        titleLbl.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(titleLbl, BorderLayout.NORTH);

        recentActivityModel = new DefaultTableModel(new Object[]{ "Invoice Number", "Total Bill", "Time" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(recentActivityModel);
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);

        // No JScrollPane: only RECENT_ACTIVITY_ROWS rows are ever loaded, so the
        // table's natural height fits without needing to scroll.
        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setBorder(BorderFactory.createLineBorder(new Color(235, 235, 235)));
        tableWrapper.add(table.getTableHeader(), BorderLayout.NORTH);
        tableWrapper.add(table, BorderLayout.CENTER);
        panel.add(tableWrapper, BorderLayout.CENTER);

        return panel;
    }

    // ============================================================
    //  DASHBOARD DATA (live refresh)
    // ============================================================

    private void loadDashboardStats() {
        // Today's summary cards
        DashboardDAO.TodayStats stats = dashboardDAO.getTodayStats();
        revenueValue.setText("Rs. " + String.format("%.2f", stats.todayRevenue));
        invoicesValue.setText(String.valueOf(stats.todayInvoices));
        medicineFeeValue.setText("Rs. " + String.format("%.2f", stats.medicineFeeToday));
        doctorFeeValue.setText("Rs. " + String.format("%.2f", stats.doctorFeeToday));
        testFeeValue.setText("Rs. " + String.format("%.2f", stats.testFeeToday));

        // Low Stock alert list
        lowStockListModel.clear();
        List<DashboardDAO.LowStockItem> lowStock = dashboardDAO.getLowStockItems();
        if (lowStock.isEmpty()) {
            lowStockListModel.addElement("No low-stock medicines right now.");
        } else {
            for (DashboardDAO.LowStockItem item : lowStock) {
                lowStockListModel.addElement(item.medicineName + " — only " + item.quantity + " left");
            }
        }
        lowStockMarqueeIndex = 0;

        // Expiring Medicines alert list
        expiringListModel.clear();
        List<DashboardDAO.ExpiringItem> expiring = dashboardDAO.getExpiringItems();
        if (expiring.isEmpty()) {
            expiringListModel.addElement("No medicines expiring soon.");
        } else {
            for (DashboardDAO.ExpiringItem item : expiring) {
                expiringListModel.addElement(item.medicineName + " — " + item.status);
            }
        }
        expiringMarqueeIndex = 0;

        // Recent Activity table
        recentActivityModel.setRowCount(0);
        SimpleDateFormat timeFormat = new SimpleDateFormat("dd MMM, hh:mm a");
        List<SalesDAO.RecentSale> recentSales = salesDAO.getRecentSales(RECENT_ACTIVITY_ROWS);
        for (SalesDAO.RecentSale sale : recentSales) {
            recentActivityModel.addRow(new Object[]{
                sale.invoiceNo,
                "Rs. " + String.format("%.2f", sale.totalBill),
                sale.time != null ? timeFormat.format(sale.time) : ""
            });
        }
        
    }
    

    public void refreshDashboard() {
        loadDashboardStats();
    }

    /** Called from other screens (MedicineManager, SalesFrame) whenever data
     *  changes, so the dashboard cards update live without reopening Home. */
    public static void refreshDashboardStatic() {
        if (instance != null) {
            instance.loadDashboardStats();
        }
    }

    // ============================================================
    //  BUTTON ACTIONS
    // ============================================================

    private void attachButtonActions() {
        medicineBtn.addActionListener(e -> {
            setActiveButton(medicineBtn);
            MedicineManager.open(role);
        });

        lowStockBtn.addActionListener(e -> {
            setActiveButton(lowStockBtn);
            new LowStockFrame().show();
        });

        expiryBtn.addActionListener(e -> {
            setActiveButton(expiryBtn);
            new ExpiryAlert().showExpiry();
        });

        salesBtn.addActionListener(e -> {
            setActiveButton(salesBtn);
            new SalesFrame();
        });

        historyBtn.addActionListener(e -> {
            setActiveButton(historyBtn);
            new SalesHistoryFrame().show();
        });

        reportsBtn.addActionListener(e -> {
            setActiveButton(reportsBtn);
            new ReportsFrame().show();
        });

        settingsBtn.addActionListener(e -> {
            setActiveButton(settingsBtn);
            new SettingsFrame().show();
        });

        logoutBtn.addActionListener(e -> {
            setActiveButton(logoutBtn);

            int option = JOptionPane.showConfirmDialog(
                    frame,
                    "Logout?",
                    "Confirm",
                    JOptionPane.YES_NO_OPTION
            );

            if (option == JOptionPane.YES_OPTION) {
                stopMarquees();
                frame.dispose();
                new LoginFrame();
            }
        });
    }

}
