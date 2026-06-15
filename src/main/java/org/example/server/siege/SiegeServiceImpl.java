package org.example.server.siege;

import org.example.common.model.Article;
import org.example.common.model.Facture;
import org.example.common.model.LigneFacture;
import org.example.common.rmi.SiegeService;
import org.example.common.util.DatabaseConnection;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SiegeServiceImpl extends UnicastRemoteObject implements SiegeService {

    public SiegeServiceImpl() throws RemoteException {
        super();
    }

    // =========================================================
    //  Archivage des factures en base de données (serveur central)
    // =========================================================

    @Override
    public void sauvegarderFactures(List<Facture> factures) throws RemoteException {
        if (factures == null || factures.isEmpty()) {
            System.out.println("Aucune facture à archiver.");
            return;
        }

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            String sqlFacture = "INSERT IGNORE INTO factures " +
                    "(id, client_id, date_facturation, total, mode_paiement, payee) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            String sqlLigne = "INSERT INTO lignes_facture " +
                    "(facture_id, ref_article, nom_article, quantite, prix_unitaire, sous_total) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            PreparedStatement stmtF = conn.prepareStatement(sqlFacture);
            PreparedStatement stmtL = conn.prepareStatement(sqlLigne);

            int nouvelles = 0;
            for (Facture f : factures) {
                stmtF.setLong(1, f.getId());
                stmtF.setString(2, f.getClientId());
                stmtF.setDate(3, Date.valueOf(f.getDateFacturation()));
                stmtF.setBigDecimal(4, f.getTotalFacture());
                stmtF.setString(5, f.getModePaiement());
                stmtF.setBoolean(6, f.isPayee());
                int rows = stmtF.executeUpdate();

                if (rows > 0) {
                    nouvelles++;
                    for (LigneFacture ligne : f.getLignes()) {
                        stmtL.setLong(1, f.getId());
                        stmtL.setString(2, ligne.getReferenceArticle());
                        stmtL.setString(3, ligne.getNomArticle());
                        stmtL.setInt(4, ligne.getQuantite());
                        stmtL.setBigDecimal(5, ligne.getPrixUnitaire());
                        stmtL.setBigDecimal(6, ligne.getSousTotal());
                        stmtL.addBatch();
                    }
                    stmtL.executeBatch();
                }
            }

            conn.commit();
            conn.setAutoCommit(true);
            System.out.println("[SIÈGE] " + nouvelles + " nouvelle(s) facture(s) archivée(s) en BD " +
                    "(" + (factures.size() - nouvelles) + " déjà présente(s)).");

        } catch (SQLException e) {
            try {
                Connection conn = DatabaseConnection.getInstance().getConnection();
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {}
            throw new RemoteException("Erreur lors de l'archivage des factures en BD", e);
        }
    }

    // =========================================================
    //  Mise à jour des prix en BD
    // =========================================================

    @Override
    public void mettreAJourPrix(Map<String, BigDecimal> nouveauPrix) throws RemoteException {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String sql = "UPDATE articles SET prix_unitaire = ? WHERE ref = ?";
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

    // =========================================================
    //  Articles & stock
    // =========================================================

    @Override
    public List<Article> getTousLesArticles() throws RemoteException {
        List<Article> articles = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String sql = "SELECT a.ref, a.nom, f.nom as famille_nom, a.prix_unitaire, a.stock " +
                         "FROM articles a LEFT JOIN familles f ON a.famille_id = f.id";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                articles.add(new Article(
                    rs.getString("ref"),
                    rs.getString("nom"),
                    rs.getString("famille_nom"),
                    rs.getBigDecimal("prix_unitaire"),
                    rs.getInt("stock")
                ));
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
            String sql = "UPDATE articles SET stock = ? WHERE ref = ?";
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

}