package com.mycompany.pharmacypos;


import javax.swing.*;
import java.awt.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;

public class HomeFrame {
private static HomeFrame instance;
    JFrame frame;

JPanel contentPanel;

JPanel card1;
JPanel card2;
JPanel card3;

JLabel medicinesValue;
JLabel stockValue;
JLabel salesValue;    

JLabel medicinesTitle;
JLabel stockTitle;
JLabel salesTitle;

    String role;
JButton medicineBtn;
JButton lowStockBtn;
JButton expiryBtn;
JButton salesBtn;
JButton historyBtn;
JButton logoutBtn;
private JButton activeButton = null;

public HomeFrame(String role) {

    instance = this;

    this.role = role;

    initializeUI();
}

  private void initializeUI() {

    frame = new JFrame("Health Haven POS");
    frame.setSize(1200, 700);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(null);
    frame.setLocationRelativeTo(null);

    createSidebar();
    createContentArea();   // 👈 MUST call ONLY this
attachButtonActions();
    frame.setVisible(true);
}
          private void createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(null);
        sidebar.setBounds(0,0,250,700);
        sidebar.setBackground(new Color(11, 40, 55));
        frame.add(sidebar);

        JLabel clinicName = new JLabel("🏥 HEALTH HAVEN");
        clinicName.setForeground(Color.WHITE);
        clinicName.setFont(new Font("Segoe UI",Font.BOLD,24));
        clinicName.setBounds(20,25,220,35);
        sidebar.add(clinicName);

        JLabel subTitle = new JLabel("Clinic Pharmacy");
        subTitle.setForeground(new Color(220,220,220));
        subTitle.setFont(new Font("Segoe UI",Font.PLAIN,15));
        subTitle.setBounds(22,58,180,20);
        sidebar.add(subTitle);

        JSeparator separator = new JSeparator();
        separator.setBounds(20,90,200,3);
        sidebar.add(separator);

        // Buttons
        medicineBtn = createMenuButton("Medicines", 110);
        lowStockBtn = createMenuButton("Low Stock", 160);
        expiryBtn = createMenuButton("Expiry Alert", 210);
        salesBtn = createMenuButton("Sales", 260);
        historyBtn = createMenuButton("Sales History", 310);
        logoutBtn = createMenuButton("Logout", 510);

        sidebar.add(medicineBtn);
        sidebar.add(lowStockBtn);
        sidebar.add(expiryBtn);
        sidebar.add(salesBtn);
        sidebar.add(historyBtn);
        sidebar.add(logoutBtn);

        styleButton(medicineBtn);
        styleButton(lowStockBtn);
        styleButton(expiryBtn);
        styleButton(salesBtn);
        styleButton(historyBtn);
        styleButton(logoutBtn);
    }
    
   private JButton createMenuButton(String text, int y) {

    JButton btn = new JButton(text);

    btn.setBounds(18, y, 214, 44);

    btn.setFont(new Font("Segoe UI", Font.BOLD, 15));

    btn.setHorizontalAlignment(SwingConstants.LEFT);

    btn.setMargin(new Insets(0,18,0,0));

    btn.setFocusPainted(false);

    btn.setBorderPainted(false);

    btn.setContentAreaFilled(true);

    btn.setBackground(new Color(22,78,99));

    btn.setForeground(Color.WHITE);

    btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

    btn.addMouseListener(new java.awt.event.MouseAdapter() {

        @Override
        public void mouseEntered(java.awt.event.MouseEvent evt) {

            btn.setBackground(new Color(34,120,150));

        }

        @Override
        public void mouseExited(java.awt.event.MouseEvent evt) {

            btn.setBackground(new Color(22,78,99));

        }

    });

    return btn;
}
  private void createContentArea() {

    contentPanel = new JPanel();
    contentPanel.setLayout(null);
    contentPanel.setBounds(250,0,950,700);
    contentPanel.setBackground(new Color(245,248,250));

    JLabel heading = new JLabel("HEALTH HAVEN CLINIC");
    heading.setBounds(60,30,500,40);
    heading.setFont(new Font("Segoe UI",Font.BOLD,32));
    heading.setForeground(new Color(0,102,102));

    contentPanel.add(heading);

    JLabel subHeading = new JLabel("Pharmacy Management System");
    subHeading.setBounds(62,70,400,30);
    subHeading.setFont(new Font("Segoe UI",Font.PLAIN,20));
    subHeading.setForeground(Color.GRAY);

    contentPanel.add(subHeading);

    JLabel welcome = new JLabel("Welcome, " + LoginFrame.loggedInUser);
    welcome.setBounds(62,120,400,30);
    welcome.setFont(new Font("Segoe UI",Font.BOLD,18));
    welcome.setForeground(new Color(60,60,60));

    contentPanel.add(welcome);

    // ⭐ IMPORTANT: cards must be created here
    createDashboardCards();

    frame.add(contentPanel);
}
      private void createDashboardCards() {
    card1 = new JPanel();
    card2 = new JPanel();
    card3 = new JPanel();

    card1.setLayout(null);
    card2.setLayout(null);
    card3.setLayout(null);
    card1.setBackground(new Color(255, 255, 255));
    card2.setBackground(new Color(255, 255, 255));
    card3.setBackground(new Color(255, 255, 255));
    card1.setBorder(BorderFactory.createLineBorder(new Color(220,220,220)));
    card2.setBorder(BorderFactory.createLineBorder(new Color(220,220,220)));
    card3.setBorder(BorderFactory.createLineBorder(new Color(220,220,220)));

    card1.setBounds(60,180,230,130);
    card2.setBounds(340,180,230,130);
    card3.setBounds(620,180,230,130);

    contentPanel.add(card1);
    contentPanel.add(card2);
    contentPanel.add(card3);
   
    // Hover effect rakho (optional, lekin clickable nahi)
    addHoverEffect(card1);
    addHoverEffect(card2);
    addHoverEffect(card3);

    JPanel accent1 = new JPanel();
    accent1.setBounds(0,0,8,130);
    accent1.setBackground(new Color(0,150,136));
    card1.add(accent1);

    JPanel accent2 = new JPanel();
    accent2.setBounds(0,0,8,130);
    accent2.setBackground(new Color(33,150,243));
    card2.add(accent2);

    JPanel accent3 = new JPanel();
    accent3.setBounds(0,0,8,130);
    accent3.setBackground(new Color(255,152,0));
    card3.add(accent3);

    // ================= CARD 1 =================
    medicinesTitle = new JLabel("Medicines");
    medicinesTitle.setFont(new Font("Segoe UI",Font.BOLD,18));
    medicinesTitle.setBounds(20,20,150,25);
    medicinesValue = new JLabel("0");
    medicinesValue.setFont(new Font("Segoe UI",Font.BOLD,36));
    medicinesValue.setBounds(20,60,150,40);
    card1.add(medicinesTitle);
    card1.add(medicinesValue);

    // ================= CARD 2 =================
    stockTitle = new JLabel("Total Stock");
    stockTitle.setFont(new Font("Segoe UI",Font.BOLD,18));
    stockTitle.setBounds(20,20,150,25);
    stockValue = new JLabel("0");
    stockValue.setFont(new Font("Segoe UI",Font.BOLD,36));
    stockValue.setBounds(20,60,150,40);
    card2.add(stockTitle);
    card2.add(stockValue);

    // ================= CARD 3 =================
    salesTitle = new JLabel("Total Sales");
    salesTitle.setFont(new Font("Segoe UI",Font.BOLD,18));
    salesTitle.setBounds(20,20,150,25);
    salesValue = new JLabel("Rs. 0");
    salesValue.setFont(new Font("Segoe UI",Font.BOLD,28));
    salesValue.setBounds(20,60,180,40);
    card3.add(salesTitle);
    card3.add(salesValue);
  
    loadDashboardStats();
}
   private void loadDashboardStats() {

    try {

        Connection con = DBConnection.getConnection();

        ResultSet rs1 =
                con.createStatement().executeQuery(
                        "SELECT COUNT(*) FROM medicines"
                );

        if(rs1.next()){

            medicinesValue.setText(
                    String.valueOf(rs1.getInt(1))
            );

        }

        ResultSet rs2 =
                con.createStatement().executeQuery(
                        "SELECT SUM(quantity) FROM medicines"
                );

        if(rs2.next()){

            stockValue.setText(
                    String.valueOf(rs2.getInt(1))
            );

        }

        ResultSet rs3 =
                con.createStatement().executeQuery(
                        "SELECT SUM(total_bill) FROM sales"
                );

        if(rs3.next()){

            salesValue.setText(
                    "Rs. " + rs3.getInt(1)
            );

        }

    }

    catch(Exception ex){

        ex.printStackTrace();

    }

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

    JButton[] buttons = {
        medicineBtn,
       lowStockBtn, salesBtn,
        historyBtn, logoutBtn
    };

    for (JButton btn : buttons) {

        btn.setBackground(new Color(22, 78, 99));

    }

    activeBtn.setBackground(new Color(0, 150, 136));
}
  private void addHoverEffect(JPanel card) {

    card.addMouseListener(new java.awt.event.MouseAdapter() {

        public void mouseEntered(java.awt.event.MouseEvent e) {
            card.setBackground(new Color(230, 245, 255));
        }

        public void mouseExited(java.awt.event.MouseEvent e) {
            card.setBackground(Color.WHITE);
        }
    });
}
  public void refreshDashboard() {
    loadDashboardStats();
}
  private void attachButtonActions() {

    medicineBtn.addActionListener(e -> {
        setActiveButton(medicineBtn);
         MedicineManager.open(); 
    });
        lowStockBtn.addActionListener(e -> {
        setActiveButton(lowStockBtn);
        showLowStock();
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
        showSalesHistory();
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
            frame.dispose();
            new LoginFrame();
        }
        
    });
}
  public static void refreshDashboardStatic() {

    if(instance != null){

        instance.loadDashboardStats();

    }

}
      private void showSalesHistory() {
        JFrame salesFrame = new JFrame("Sales History");
        salesFrame.setSize(1050, 600);
        salesFrame.setLocationRelativeTo(null);
        salesFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        String[] columns = {
            "ID", 
            "Medicine", 
            "Quantity", 
            "Sale Price", 
            "Total Bill", 
            "Invoice No", 
            "Expiry Date", 
            "Sale Date"
        };

        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.setBounds(20, 20, 1000, 480);
        salesFrame.add(sp);

        JLabel totalSalesLabel = new JLabel("Total Sales: Rs. 0.00");
        totalSalesLabel.setBounds(20, 520, 400, 30);
        totalSalesLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        salesFrame.add(totalSalesLabel);

        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT * FROM sales ORDER BY sale_date DESC";
            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            double totalSales = 0;

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("medicine_name"),
                    rs.getInt("quantity"),
                    rs.getDouble("sale_price"),
                    rs.getDouble("total_bill"),
                    rs.getString("invoice_no"),
                    rs.getDate("expiry_date"),
                    rs.getTimestamp("sale_date")
                });
                totalSales += rs.getDouble("total_bill");
            }

            totalSalesLabel.setText("Total Sales: Rs. " + String.format("%.2f", totalSales));

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(salesFrame, "Error loading sales history: " + ex.getMessage());
        }

        salesFrame.setVisible(true);
    }
          private void showLowStock() {
        JFrame lowFrame = new JFrame("Low Stock Medicines");
        lowFrame.setSize(900, 550);
        lowFrame.setLocationRelativeTo(null);
        lowFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        String[] columns = {"ID", "Medicine", "Company", "Quantity", "Expiry Date"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.setBounds(20, 20, 840, 450);
        lowFrame.add(sp);

        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT * FROM medicines WHERE quantity < 100 ORDER BY quantity ASC";
            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            int count = 0;
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("medicine_name"),
                    rs.getString("company"),
                    rs.getInt("quantity"),
                    rs.getDate("expiry_date")
                });
                count++;
            }

            if (count == 0) {
                JOptionPane.showMessageDialog(lowFrame, "No medicines with quantity less than 100!");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(lowFrame, "Error loading low stock data!");
        }

        lowFrame.setVisible(true);
    }
}