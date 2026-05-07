package com.shopflow.shopflow_backend;

import java.sql.*;

public class TestSQLConnection {
    public static void main(String[] args) {
        // Essaie les 3 URLs une par une
        String[] urls = {
                // Option 1: Instance nommée (recommandé)
                "jdbc:sqlserver://localhost\\\\SQLEXPRESS;databaseName=ShopFlowDB;integratedSecurity=true;encrypt=false;",

                // Option 2: Localhost sans port (Named Pipes)
                "jdbc:sqlserver://localhost;databaseName=ShopFlowDB;integratedSecurity=true;encrypt=false;",

                // Option 3: Avec port
                "jdbc:sqlserver://localhost:1433;databaseName=ShopFlowDB;integratedSecurity=true;encrypt=false;"
        };

        for (String url : urls) {
            System.out.println("Testing: " + url);
            try (Connection conn = DriverManager.getConnection(url)) {
                System.out.println("✅ SUCCESS: Connected to: " + conn.getMetaData().getURL());
                return;
            } catch (SQLException e) {
                System.out.println("❌ FAILED: " + e.getMessage());
            }
        }
    }
}