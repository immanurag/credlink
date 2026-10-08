package com.credlink;

import java.sql.*;

public class InspectMerchants {
    public static void main(String[] args) {
        String url = "jdbc:h2:file:./data/credlink;AUTO_SERVER=TRUE";
        String user = "credlink";
        String pass = "changeme";

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("--- MERCHANTS ---");
            PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM MERCHANTS");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.printf("Merchant ID: %d | Email: %s | Store: %s\n", rs.getLong("id"), rs.getString("email"), rs.getString("store_name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
