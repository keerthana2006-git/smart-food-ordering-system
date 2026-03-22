// SmartFoodOrderingSystemJDBC.java
// Connected with MySQL Database
// Database: SmartFoodOrderingSystem
// Username: root
// Password: MyNewPassword2006!

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.sql.Date;
import java.util.*;

// -------------------- Database Connection --------------------
class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/SmartFoodOrderingSystem";
    private static final String USER = "root";
    private static final String PASSWORD = "MyNewPassword2006!";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}

// -------------------- Abstract Class --------------------
abstract class MenuItem {
    protected int id;
    protected String itemName;
    protected double price;

    public MenuItem(int id, String itemName, double price) {
        this.id = id;
        this.itemName = itemName;
        this.price = price;
    }

    public int getId() { return id; }
    public String getItemName() { return itemName; }
    public double getPrice() { return price; }

    public abstract String showDetails();
}

// -------------------- Derived Classes --------------------
class MainCourse extends MenuItem {
    private boolean isVeg;
    public MainCourse(int id, String itemName, double price, boolean isVeg) {
        super(id, itemName, price);
        this.isVeg = isVeg;
    }
    @Override
    public String showDetails() {
        return itemName + (isVeg ? " (Veg)" : " (Non-Veg)") + " - ₹" + price;
    }
}

class Beverage extends MenuItem {
    private String size;
    public Beverage(int id, String itemName, double price, String size) {
        super(id, itemName, price);
        this.size = size;
    }
    @Override
    public String showDetails() {
        return itemName + " [" + size + "] - ₹" + price;
    }
}

class Dessert extends MenuItem {
    private String info;
    public Dessert(int id, String itemName, double price, String info) {
        super(id, itemName, price);
        this.info = info;
    }
    @Override
    public String showDetails() {
        return itemName + " (" + info + ") - ₹" + price;
    }
}

class Bread extends MenuItem {
    private String info;
    public Bread(int id, String itemName, double price, String info) {
        super(id, itemName, price);
        this.info = info;
    }
    @Override
    public String showDetails() {
        return itemName + " [" + info + "] - ₹" + price;
    }
}

// -------------------- Order Class --------------------
class Order {
    private static int orderCounter = 1000;
    private int orderId;
    private ArrayList<MenuItem> orderedItems;
    private Date orderTime;

    public Order() {
        this.orderId = ++orderCounter;
        this.orderedItems = new ArrayList<>();
        this.orderTime = new Date(orderId, orderId, orderId);
    }

    public void addItem(MenuItem item) { orderedItems.add(item); }
    public void removeItem(int index) {
        if (index >= 0 && index < orderedItems.size()) orderedItems.remove(index);
    }
    public double calculateTotal() {
        double total = 0;
        for (MenuItem m : orderedItems) total += m.getPrice();
        return total;
    }
    public ArrayList<MenuItem> getItems() { return orderedItems; }
    public int getOrderId() { return orderId; }
    public Date getOrderTime() { return orderTime; }
}

// -------------------- Menu Data Loader --------------------
class MenuData {
    private HashMap<String, ArrayList<MenuItem>> menuList;

    public MenuData() {
        menuList = new HashMap<>();
        loadMenuFromDatabase();
    }

    private void loadMenuFromDatabase() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT * FROM menu_items");

            while (rs.next()) {
                int id = rs.getInt("item_id");
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                String category = rs.getString("category");
                String extra = rs.getString("extra_info");

                MenuItem item = null;
                switch (category.toLowerCase()) {
                    case "main course":
                        item = new MainCourse(id, name, price, "Veg".equalsIgnoreCase(extra));
                        break;
                    case "beverages":
                        item = new Beverage(id, name, price, extra);
                        break;
                    case "dessert":
                        item = new Dessert(id, name, price, extra);
                        break;
                    case "bread":
                        item = new Bread(id, name, price, extra);
                        break;
                    default:
                        item = new Beverage(id, name, price, extra);
                        break;
                }

                menuList.computeIfAbsent(category, k -> new ArrayList<>()).add(item);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error loading menu: " + e.getMessage());
        }
    }

    public HashMap<String, ArrayList<MenuItem>> getMenu() { return menuList; }
    public ArrayList<MenuItem> getItemsByCategory(String category) {
        return menuList.getOrDefault(category, new ArrayList<>());
    }
}

// -------------------- GUI Class --------------------
public class SmartFoodOrderingSystem {
    private JFrame window;
    private JComboBox<String> categoryBox, itemBox;
    private DefaultTableModel cartModel;
    private JLabel totalLabel;
    private MenuData menuData;
    private Order currentOrder;

    public SmartFoodOrderingSystem() {
        menuData = new MenuData();
        currentOrder = new Order();
        setupInterface();
    }

    private void setupInterface() {
        window = new JFrame("Smart Food Ordering System (MySQL Version)");
        window.setSize(800, 500);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLocationRelativeTo(null);

        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);

        categoryBox = new JComboBox<>();
        for (String cat : menuData.getMenu().keySet()) categoryBox.addItem(cat);
        categoryBox.addActionListener(e -> updateItemBox());

        itemBox = new JComboBox<>();
        updateItemBox();

        JButton addButton = new JButton("Add to Cart");
        addButton.addActionListener(e -> addItemToCart());

        gbc.gridx=0; gbc.gridy=0;
        topPanel.add(new JLabel("Category:"), gbc);
        gbc.gridx=1;
        topPanel.add(categoryBox, gbc);
        gbc.gridx=0; gbc.gridy=1;
        topPanel.add(new JLabel("Item:"), gbc);
        gbc.gridx=1;
        topPanel.add(itemBox, gbc);
        gbc.gridx=2; gbc.gridy=0; gbc.gridheight=2;
        topPanel.add(addButton, gbc);

        cartModel = new DefaultTableModel(new Object[]{"#", "Item", "Price"}, 0);
        JTable cartTable = new JTable(cartModel);
        JScrollPane tablePane = new JScrollPane(cartTable);

        JPanel sidePanel = new JPanel(new GridLayout(6, 1, 5, 5));
        totalLabel = new JLabel("Total: ₹0.00", SwingConstants.CENTER);
        totalLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JButton removeBtn = new JButton("Remove Selected");
        removeBtn.addActionListener(e -> {
            int row = cartTable.getSelectedRow();
            if (row >= 0) {
                currentOrder.removeItem(row);
                cartModel.removeRow(row);
                updateTotal();
            }
        });

        JButton clearBtn = new JButton("Clear Cart");
        clearBtn.addActionListener(e -> {
            currentOrder = new Order();
            cartModel.setRowCount(0);
            updateTotal();
        });

        JButton placeBtn = new JButton("Place Order");
        placeBtn.addActionListener(e -> confirmAndPlaceOrder());

        sidePanel.add(totalLabel);
        sidePanel.add(removeBtn);
        sidePanel.add(clearBtn);
        sidePanel.add(placeBtn);

        window.setLayout(new BorderLayout());
        window.add(topPanel, BorderLayout.NORTH);
        window.add(tablePane, BorderLayout.CENTER);
        window.add(sidePanel, BorderLayout.EAST);

        window.setVisible(true);
    }

    private void updateItemBox() {
        itemBox.removeAllItems();
        String category = (String) categoryBox.getSelectedItem();
        if (category != null) {
            for (MenuItem m : menuData.getItemsByCategory(category))
                itemBox.addItem(m.getId() + " - " + m.showDetails());
        }
    }

    private void addItemToCart() {
        try {
            String selected = (String) itemBox.getSelectedItem();
            if (selected == null) throw new Exception("No item selected!");
            int id = Integer.parseInt(selected.split("-")[0].trim());
            MenuItem chosen = findItemById(id);
            if (chosen == null) throw new Exception("Item not found!");

            currentOrder.addItem(chosen);
            cartModel.addRow(new Object[]{cartModel.getRowCount() + 1, chosen.getItemName(), "₹" + chosen.getPrice()});
            updateTotal();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(window, "Error: " + ex.getMessage());
        }
    }

    private MenuItem findItemById(int id) {
        for (ArrayList<MenuItem> list : menuData.getMenu().values())
            for (MenuItem m : list)
                if (m.getId() == id) return m;
        return null;
    }

    private void updateTotal() {
        totalLabel.setText(String.format("Total: ₹%.2f", currentOrder.calculateTotal()));
    }

    private void confirmAndPlaceOrder() {
        if (currentOrder.getItems().isEmpty()) {
            JOptionPane.showMessageDialog(window, "Cart is empty!");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            String orderSQL = "INSERT INTO orders (order_time, total_amount) VALUES (NOW(), ?)";
            PreparedStatement psOrder = conn.prepareStatement(orderSQL, Statement.RETURN_GENERATED_KEYS);
            psOrder.setDouble(1, currentOrder.calculateTotal());
            psOrder.executeUpdate();

            ResultSet rs = psOrder.getGeneratedKeys();
            int orderId = 0;
            if (rs.next()) orderId = rs.getInt(1);

            String itemSQL = "INSERT INTO order_items (order_id, item_id, quantity, price) VALUES (?, ?, ?, ?)";
            PreparedStatement psItem = conn.prepareStatement(itemSQL);

            for (MenuItem item : currentOrder.getItems()) {
                psItem.setInt(1, orderId);
                psItem.setInt(2, item.getId());
                psItem.setInt(3, 1);
                psItem.setDouble(4, item.getPrice());
                psItem.executeUpdate();
            }

            conn.commit();
            JOptionPane.showMessageDialog(window, "Order placed successfully!");
            currentOrder = new Order();
            cartModel.setRowCount(0);
            updateTotal();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(window, "Database Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(SmartFoodOrderingSystem::new);
    }
}