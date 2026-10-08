import java.sql.*;

public class InspectDatabase {

    public static void main(String[] args) throws Exception {
        String dbUrl = "jdbc:h2:file:./data/credlink;MODE=MySQL;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE";
        Class.forName("org.h2.Driver");

        try (Connection conn = DriverManager.getConnection(dbUrl, "sa", "")) {
            System.out.println("Connected to H2 database.");

            // 1. Inspect Customers table
            System.out.println("\n=== CUSTOMERS TABLE ===");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, merchant_id, name, phone, current_balance FROM customers")) {
                while (rs.next()) {
                    System.out.println(String.format("ID: %d | MerchantID: %d | Name: %s | Phone: %s | CurrentBalance: %s",
                            rs.getLong("id"),
                            rs.getLong("merchant_id"),
                            rs.getString("name"),
                            rs.getString("phone"),
                            rs.getBigDecimal("current_balance")));
                }
            }

            // 2. Inspect Transactions table
            System.out.println("\n=== TRANSACTIONS TABLE ===");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, merchant_id, customer_id, type, amount, balance_after, status, description, created_at FROM transactions ORDER BY id ASC")) {
                while (rs.next()) {
                    System.out.println(String.format("ID: %d | MerchantID: %d | CustomerID: %d | Type: %s | Amount: %s | BalanceAfter: %s | Status: %s | Desc: %s | CreatedAt: %s",
                            rs.getLong("id"),
                            rs.getLong("merchant_id"),
                            rs.getLong("customer_id"),
                            rs.getString("type"),
                            rs.getBigDecimal("amount"),
                            rs.getBigDecimal("balance_after"),
                            rs.getString("status"),
                            rs.getString("description"),
                            rs.getTimestamp("created_at")));
                }
            }
        }
    }
}
