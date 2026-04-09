package org.example.server.headquarters;

import org.example.common.model.Article;
import org.example.common.model.Facture;
import org.example.common.model.LigneFacture;
import org.example.common.rmi.SiegeService;
import org.example.common.util.DatabaseConnection;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SiegeServiceImpl extends UnicastRemoteObject implements SiegeService {

    public SiegeServiceImpl() throws RemoteException {
        super();
    }

    @Override
    public void mettreAJourPrix(Map<String, BigDecimal> nouveauPrix) throws RemoteException {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String sql = "UPDATE articles SET prix_unitaire = ? WHERE reference = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            for (Map.Entry<String, BigDecimal> entry : nouveauPrix.entrySet()) {
                stmt.setBigDecimal(1, entry.getValue());
                stmt.setString(2, entry.getKey());
                stmt.addBatch();
            }

            stmt.executeBatch();
            System.out.println("Mise à jour des prix effectuée pour " + nouveauPrix.size() + " articles");

        } catch (SQLException e) {
            throw new RemoteException("Erreur lors de la mise à jour des prix", e);
        }
    }

    @Override
    public void sauvegarderFactures(List<Facture> factures) throws RemoteException {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            for (Facture facture : factures) {
                // Vérifier si la facture existe déjà
                if (factureExiste(facture.getId())) {
                    continue;
                }

                // Insérer la facture
                String insertFactureSql = "INSERT INTO factures (id, client_id, total_facture, mode_paiement, date_facturation, payee) VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement factureStmt = conn.prepareStatement(insertFactureSql);
                factureStmt.setLong(1, facture.getId());
                factureStmt.setString(2, facture.getClientId());
                factureStmt.setBigDecimal(3, facture.getTotalFacture());
                factureStmt.setString(4, facture.getModePaiement());
                factureStmt.setDate(5, Date.valueOf(facture.getDateFacturation()));
                factureStmt.setBoolean(6, facture.isPayee());
                factureStmt.executeUpdate();

                // Insérer les lignes de facture
                String insertLigneSql = "INSERT INTO lignes_facture (facture_id, reference_article, nom_article, quantite, prix_unitaire, sous_total) VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement ligneStmt = conn.prepareStatement(insertLigneSql);

                for (LigneFacture ligne : facture.getLignes()) {
                    ligneStmt.setLong(1, facture.getId());
                    ligneStmt.setString(2, ligne.getReferenceArticle());
                    ligneStmt.setString(3, ligne.getNomArticle());
                    ligneStmt.setInt(4, ligne.getQuantite());
                    ligneStmt.setBigDecimal(5, ligne.getPrixUnitaire());
                    ligneStmt.setBigDecimal(6, ligne.getSousTotal());
                    ligneStmt.addBatch();
                }
                ligneStmt.executeBatch();
            }

            conn.commit();
            System.out.println("Sauvegarde de " + factures.size() + " factures effectuée");

        } catch (SQLException e) {
            throw new RemoteException("Erreur lors de la sauvegarde des factures", e);
        }
    }

    private boolean factureExiste(Long factureId) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        String sql = "SELECT COUNT(*) FROM factures WHERE id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setLong(1, factureId);
        ResultSet rs = stmt.executeQuery();
        rs.next();
        return rs.getInt(1) > 0;
    }

    @Override
    public List<Article> getTousLesArticles() throws RemoteException {
        List<Article> articles = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String sql = "SELECT * FROM articles";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Article article = new Article(
                    rs.getString("reference"),
                    rs.getString("famille"),
                    rs.getBigDecimal("prix_unitaire"),
                    rs.getInt("quantite_stock")
                );
                articles.add(article);
            }

        } catch (SQLException e) {
            throw new RemoteException("Erreur lors de la récupération des articles", e);
        }
        return articles;
    }

    @Override
    public void synchroniserStock(List<Article> articles) throws RemoteException {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String sql = "UPDATE articles SET quantite_stock = ? WHERE reference = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            for (Article article : articles) {
                stmt.setInt(1, article.getQuantiteEnStock());
                stmt.setString(2, article.getReference());
                stmt.addBatch();
            }

            stmt.executeBatch();
            System.out.println("Synchronisation du stock effectuée pour " + articles.size() + " articles");

        } catch (SQLException e) {
            throw new RemoteException("Erreur lors de la synchronisation du stock", e);
        }
    }

    @Override
    public BigDecimal calculerChiffreAffairesTotal(LocalDate date) throws RemoteException {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String sql = "SELECT SUM(total_facture) as chiffre_total FROM factures WHERE date_facturation = ? AND payee = TRUE";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setDate(1, Date.valueOf(date));
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                BigDecimal chiffre = rs.getBigDecimal("chiffre_total");
                return chiffre != null ? chiffre : BigDecimal.ZERO;
            }
            return BigDecimal.ZERO;

        } catch (SQLException e) {
            throw new RemoteException("Erreur lors du calcul du chiffre d'affaires total", e);
        }
    }
}