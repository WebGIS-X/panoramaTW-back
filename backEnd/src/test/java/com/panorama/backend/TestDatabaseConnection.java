package com.panorama.backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDatabaseConnection {

    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/panoramaTW";
        String username = "postgres";
        String password = "040322";

        System.out.println("========================================");
        System.out.println("PostgreSQL 连接测试");
        System.out.println("========================================");
        System.out.println("URL: " + url);
        System.out.println("用户名: " + username);
        System.out.println("密码: " + password);
        System.out.println("========================================");

        Connection conn = null;
        try {
            System.out.println("\n🔄 正在连接...");
            conn = DriverManager.getConnection(url, username, password);

            if (conn != null) {
                System.out.println("✅ 连接成功！");
                System.out.println("数据库产品: " + conn.getMetaData().getDatabaseProductName());
                System.out.println("数据库版本: " + conn.getMetaData().getDatabaseProductVersion());

                // 测试查询
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT 1 as test");
                if (rs.next()) {
                    System.out.println("✅ 查询测试成功！结果: " + rs.getInt("test"));
                }
                rs.close();
                stmt.close();

                System.out.println("\n========================================");
                System.out.println("✅ PostgreSQL 连接完全正常！");
                System.out.println("========================================");
            }

        } catch (Exception e) {
            System.err.println("\n========================================");
            System.err.println("❌ 连接失败！");
            System.err.println("========================================");
            System.err.println("错误类型: " + e.getClass().getName());
            System.err.println("错误信息: " + e.getMessage());

            if (e.getMessage() != null) {
                if (e.getMessage().contains("password authentication failed")) {
                    System.err.println("\n🔴 密码错误！");
                    System.err.println("   请检查密码是否正确");
                    System.err.println("   当前使用的密码: " + password);
                } else if (e.getMessage().contains("Connection refused")) {
                    System.err.println("\n🔴 PostgreSQL 未运行或端口错误！");
                    System.err.println("   请检查 PostgreSQL 是否运行在 5432 端口");
                } else if (e.getMessage().contains("database") && e.getMessage().contains("does not exist")) {
                    System.err.println("\n🔴 数据库不存在！");
                    System.err.println("   请创建数据库: CREATE DATABASE panoramaTW;");
                }
            }

            System.err.println("\n完整堆栈:");
            e.printStackTrace();

            System.err.println("\n========================================");
            System.err.println("测试失败，请根据上面的错误信息修复");
            System.err.println("========================================");
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    System.out.println("\n连接已关闭");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
