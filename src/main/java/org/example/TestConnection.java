package org.example;

import org.example.common.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestConnection {
    public static void main(String[] args) {
        try {
            System.out.println("=== TEST COMPLET DE LA BASE DE DONNÉES ===");

            DatabaseConnection dbConn = DatabaseConnection.getInstance();
            Connection conn = dbConn.getConnection();

            System.out.println("✅ Connexion établie avec succès !");

            Statement stmt = conn.createStatement();

            // Test 1: Vérifier les tables existantes
            System.out.println("\n--- Tables disponibles ---");
            ResultSet tables = conn.getMetaData().getTables("brico_merlin", null, "%", new String[]{"TABLE"});
            while (tables.next()) {
                System.out.println("  📋 " + tables.getString("TABLE_NAME"));
            }

            // Test 2: Compter les familles
            try {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM familles");
                if (rs.next()) {
                    System.out.println("\n--- Familles ---");
                    System.out.println("✅ Nombre de familles: " + rs.getInt("count"));
                }
                rs.close();

                // Lister les familles
                rs = stmt.executeQuery("SELECT id, nom FROM familles");
                while (rs.next()) {
                    System.out.println("  🏷️ " + rs.getInt("id") + " - " + rs.getString("nom"));
                }
                rs.close();
            } catch (Exception e) {
                System.out.println("❌ Table familles inexistante: " + e.getMessage());
            }

            // Test 3: Compter et lister les articles
            try {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM articles");
                if (rs.next()) {
                    System.out.println("\n--- Articles ---");
                    System.out.println("✅ Nombre d'articles: " + rs.getInt("count"));
                }
                rs.close();

                // Lister tous les articles avec détails
                rs = stmt.executeQuery("SELECT ref, famille_id, prix_unitaire, stock FROM articles LIMIT 10");
                while (rs.next()) {
                    System.out.println("  📦 " + rs.getString("ref") +
                                     " | Famille: " + rs.getInt("famille_id") +
                                     " | Prix: " + rs.getBigDecimal("prix_unitaire") +
                                     " | Stock: " + rs.getInt("stock"));
                }
                rs.close();
            } catch (Exception e) {
                System.out.println("❌ Problème avec table articles: " + e.getMessage());
            }

            // Test 4: Vérifier les contraintes FK
            try {
                ResultSet rs = stmt.executeQuery(
                    "SELECT a.ref, a.famille_id, f.nom as famille_nom, a.prix_unitaire, a.stock " +
                    "FROM articles a LEFT JOIN familles f ON a.famille_id = f.id LIMIT 5"
                );
                System.out.println("\n--- Test jointure Articles-Familles ---");
                while (rs.next()) {
                    System.out.println("  🔗 " + rs.getString("ref") +
                                     " | " + rs.getString("famille_nom") +
                                     " | " + rs.getBigDecimal("prix_unitaire") + "€");
                }
                rs.close();
            } catch (Exception e) {
                System.out.println("❌ Problème jointure: " + e.getMessage());
            }

            stmt.close();
            System.out.println("\n=== FIN DU TEST ===");

        } catch (Exception e) {
            System.err.println("❌ ERREUR GLOBALE: " + e.getMessage());
            e.printStackTrace();
        }
    }
}