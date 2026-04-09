package org.example.server.store;

import org.example.common.model.Article;
import org.example.common.model.Facture;
import org.example.common.model.LigneFacture;
import org.example.common.rmi.MagasinService;
import org.example.common.util.DatabaseConnection;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MagasinServiceImpl extends UnicastRemoteObject implements MagasinService {

    public MagasinServiceImpl() throws RemoteException {
        super();
    }

    @Override
    public Article consulterStockArticle(String reference) throws RemoteException {
        System.out.println("🔍 Consultation article: " + reference);
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String sql = "SELECT a.ref, f.nom as famille_nom, a.prix_unitaire, a.stock FROM articles a LEFT JOIN familles f ON a.famille_id = f.id WHERE a.ref = ?";
            System.out.println("📝 SQL: " + sql);
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, reference);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Article article = new Article(
                    rs.getString("ref"),
                    rs.getString("famille_nom"),
                    rs.getBigDecimal("prix_unitaire"),
                    rs.getInt("stock")
                );
                System.out.println("✅ Article trouvé: " + article.getReference() + " | Stock: " + article.getQuantiteEnStock());
                return article;
            } else {
                System.out.println("❌ Aucun article trouvé pour: " + reference);
            }
            return null;
        } catch (SQLException e) {
            throw new RemoteException("Erreur lors de la consultation du stock", e);
        }
    }

    @Override
    public List<String> rechercherArticleParFamille(String famille) throws RemoteException {
        List<String> references = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String sql = "SELECT a.ref FROM articles a JOIN familles f ON a.famille_id = f.id WHERE f.nom = ? AND a.stock > 0";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, famille);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                references.add(rs.getString("ref"));
            }
        } catch (SQLException e) {
            throw new RemoteException("Erreur lors de la recherche d'articles", e);
        }
        return references;
    }

    @Override
    public boolean acheterArticle(String clientId, String referenceArticle, int quantite, String modePaiement) throws RemoteException {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            // Vérifier le stock disponible
            Article article = consulterStockArticle(referenceArticle);
            if (article == null || article.getQuantiteEnStock() < quantite) {
                conn.rollback();
                return false;
            }

            // Mettre à jour le stock
            String updateStockSql = "UPDATE articles SET stock = stock - ? WHERE ref = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateStockSql);
            updateStmt.setInt(1, quantite);
            updateStmt.setString(2, referenceArticle);
            updateStmt.executeUpdate();

            // Créer la facture
            String insertFactureSql = "INSERT INTO factures (client_id, total, mode_paiement, date_facturation) VALUES (?, ?, ?, ?)";
            PreparedStatement factureStmt = conn.prepareStatement(insertFactureSql, Statement.RETURN_GENERATED_KEYS);
            BigDecimal total = article.getPrixUnitaire().multiply(BigDecimal.valueOf(quantite));
            factureStmt.setString(1, clientId);
            factureStmt.setBigDecimal(2, total);
            factureStmt.setString(3, modePaiement);
            factureStmt.setDate(4, Date.valueOf(LocalDate.now()));
            factureStmt.executeUpdate();

            // Récupérer l'ID de la facture
            ResultSet generatedKeys = factureStmt.getGeneratedKeys();
            long factureId;
            if (generatedKeys.next()) {
                factureId = generatedKeys.getLong(1);
            } else {
                conn.rollback();
                return false;
            }

            // Ajouter la ligne de facture
            String insertLigneSql = "INSERT INTO lignes_facture (facture_id, ref_article, quantite, prix_unitaire) VALUES (?, ?, ?, ?)";
            PreparedStatement ligneStmt = conn.prepareStatement(insertLigneSql);
            ligneStmt.setLong(1, factureId);
            ligneStmt.setString(2, referenceArticle);
            ligneStmt.setInt(3, quantite);
            ligneStmt.setBigDecimal(4, article.getPrixUnitaire());
            ligneStmt.executeUpdate();

            conn.commit();
            return true;

        } catch (SQLException e) {
            throw new RemoteException("Erreur lors de l'achat", e);
        }
    }

    @Override
    public boolean payerFacture(Long factureId, String modePaiement) throws RemoteException {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String sql = "UPDATE factures SET statut = 'payee', mode_paiement = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, modePaiement);
            stmt.setLong(2, factureId);

            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;

        } catch (SQLException e) {
            throw new RemoteException("Erreur lors du paiement", e);
        }
    }

    @Override
    public Facture consulterFacture(Long factureId) throws RemoteException {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String sql = "SELECT * FROM factures WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, factureId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Facture facture = new Facture();
                facture.setId(rs.getLong("id"));
                facture.setClientId(rs.getString("client_id"));
                facture.setTotalFacture(rs.getBigDecimal("total"));
                facture.setModePaiement(rs.getString("mode_paiement"));
                facture.setDateFacturation(rs.getDate("date_facturation").toLocalDate());
                facture.setPayee(rs.getString("statut").equals("payee"));

                // Récupérer les lignes de facture
                String lignesQuery = "SELECT * FROM lignes_facture WHERE facture_id = ?";
                PreparedStatement lignesStmt = conn.prepareStatement(lignesQuery);
                lignesStmt.setLong(1, factureId);
                ResultSet lignesRs = lignesStmt.executeQuery();

                List<LigneFacture> lignes = new ArrayList<>();
                while (lignesRs.next()) {
                    LigneFacture ligne = new LigneFacture(
                        lignesRs.getString("ref_article"),
                        "Article " + lignesRs.getString("ref_article"),
                        lignesRs.getInt("quantite"),
                        lignesRs.getBigDecimal("prix_unitaire")
                    );
                    lignes.add(ligne);
                }
                facture.setLignes(lignes);

                return facture;
            }
            return null;

        } catch (SQLException e) {
            throw new RemoteException("Erreur lors de la consultation de facture", e);
        }
    }

    @Override
    public List<Facture> getFacturesClient(String clientId) throws RemoteException {
        List<Facture> factures = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String sql = "SELECT id FROM factures WHERE client_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, clientId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Facture facture = consulterFacture(rs.getLong("id"));
                if (facture != null) {
                    factures.add(facture);
                }
            }
        } catch (SQLException e) {
            throw new RemoteException("Erreur lors de la récupération des factures client", e);
        }
        return factures;
    }

    @Override
    public boolean ajouterStock(String referenceArticle, int quantite) throws RemoteException {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String sql = "UPDATE articles SET stock = stock + ? WHERE ref = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, quantite);
            stmt.setString(2, referenceArticle);

            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;

        } catch (SQLException e) {
            throw new RemoteException("Erreur lors de l'ajout de stock", e);
        }
    }

    @Override
    public BigDecimal calculerChiffreAffaires(LocalDate date) throws RemoteException {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String sql = "SELECT SUM(total) as chiffre_affaires FROM factures WHERE date_facturation = ? AND statut = 'payee'";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setDate(1, Date.valueOf(date));
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                BigDecimal chiffre = rs.getBigDecimal("chiffre_affaires");
                return chiffre != null ? chiffre : BigDecimal.ZERO;
            }
            return BigDecimal.ZERO;

        } catch (SQLException e) {
            throw new RemoteException("Erreur lors du calcul du chiffre d'affaires", e);
        }
    }
}