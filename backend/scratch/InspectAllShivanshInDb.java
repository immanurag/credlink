import java.sql.*;
import java.math.BigDecimal;

public class InspectAllShivanshInDb {

    public static void main(String[] args) throws Exception {
        String dbUrl = "jdbc:h2:file:./data/credlink;MODE=MySQL;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE";
        Class.forName("org.h2.Driver");

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(dbUrl, "CREDLINK", "");
        } catch (Exception e) {
            try {
                conn = DriverManager.getConnection(dbUrl, "sa", "");
            } catch (Exception e2) {
                System.out.println("Failed with CREDLINK and sa: " + e2.getMessage());
                return;
            }
        }

        try {
            System.out.println("=== DIRECT H2 DATABASE INSPECTION FOR SHIVANSH ===");

            // 1. Query all customers named Shivansh or containing Shivansh
            String custSql = "SELECT id, merchant_id, name, phone, current_balance, credit_limit, trust_score, created_at FROM customers";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(custSql)) {
                while (rs.next()) {
                    long custId = rs.getLong("id");
                    long merchantId = rs.getLong("merchant_id");
                    String name = rs.getString("name");
                    String phone = rs.getString("phone");
                    BigDecimal currentBalance = rs.getBigDecimal("current_balance");
                    BigDecimal creditLimit = rs.getBigDecimal("credit_limit");
                    int trustScore = rs.getInt("trust_score");

                    System.out.println("\n---------------------------------------------------------");
                    System.out.println("CUSTOMER RECORD:");
                    System.out.println("  ID: " + custId);
                    System.out.println("  Merchant ID: " + merchantId);
                    System.out.println("  Name: " + name);
                    System.out.println("  Phone: " + phone);
                    System.out.println("  Stored currentBalance: ₹" + currentBalance);
                    System.out.println("  Credit Limit: ₹" + creditLimit);

                    // 2. Query all transactions for this customer
                    String txSql = "SELECT id, type, amount, balance_after, status, description, created_at FROM transactions WHERE customer_id = " + custId + " ORDER BY id ASC";
                    try (Statement txStmt = conn.createStatement();
                         ResultSet txRs = txStmt.executeQuery(txSql)) {
                        double totalUdhaar = 0.0;
                        double totalJama = 0.0;
                        double totalVolume = 0.0;
                        int txCount = 0;

                        System.out.println("  TRANSACTIONS:");
                        while (txRs.next()) {
                            txCount++;
                            long txId = txRs.getLong("id");
                            String type = txRs.getString("type");
                            double amount = txRs.getDouble("amount");
                            double balanceAfter = txRs.getDouble("balance_after");
                            String status = txRs.getString("status");
                            String desc = txRs.getString("description");

                            totalVolume += amount;
                            if ("CONFIRMED".equalsIgnoreCase(status)) {
                                if ("UDHAAR".equalsIgnoreCase(type)) {
                                    totalUdhaar += amount;
                                } else if ("JAMA".equalsIgnoreCase(type)) {
                                    totalJama += amount;
                                }
                            }

                            System.out.println(String.format("    Tx #%d | ID: %d | Type: %-6s | Amount: ₹%8.2f | BalanceAfter: ₹%8.2f | Status: %-15s | Desc: %s",
                                    txCount, txId, type, amount, balanceAfter, status, desc));
                        }

                        double calculatedNetDue = totalUdhaar - totalJama;
                        System.out.println("  SUMMARY FOR CUSTOMER ID " + custId + ":");
                        System.out.println("    Total Confirmed Udhaar: ₹" + totalUdhaar);
                        System.out.println("    Total Confirmed Jama:   ₹" + totalJama);
                        System.out.println("    Calculated Net Due:     ₹" + calculatedNetDue);
                        System.out.println("    Total Transaction Volume (All txs): ₹" + totalVolume);
                    }
                }
            }
        } finally {
            if (conn != null) conn.close();
        }
    }
}
