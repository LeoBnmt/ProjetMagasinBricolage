package org.example.server.siege;

import org.example.common.model.Facture;
import org.example.common.model.LigneFacture;
import org.example.common.rmi.SiegeService;
import org.example.common.util.DatabaseConnection;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.time.LocalDate;
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

    @Override
    public void sauvegarderFactures(List<Facture> factures) throws RemoteException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            for (Facture facture : factures) {
                // Vérifier si la facture existe déjà
                if (factureExiste(facture.getId())) {
                    continue;
                }

                // Insérer la facture
                String insertFactureSql = "INSERT INTO factures (id, client_id, total, mode_paiement, date_facturation, statut) VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement factureStmt = conn.prepareStatement(insertFactureSql);
                factureStmt.setLong(1, facture.getId());
                factureStmt.setString(2, facture.getClientId());
                factureStmt.setBigDecimal(3, facture.getTotalFacture());
                factureStmt.setString(4, facture.getModePaiement());
                factureStmt.setDate(5, Date.valueOf(facture.getDateFacturation()));
                factureStmt.setString(6, facture.isPayee() ? "payee" : "en_attente");
                factureStmt.executeUpdate();

                // Insérer les lignes de facture
                String insertLigneSql = "INSERT INTO lignes_facture (facture_id, ref_article, quantite, prix_unitaire) VALUES (?, ?, ?, ?)";
                PreparedStatement ligneStmt = conn.prepareStatement(insertLigneSql);

                for (LigneFacture ligne : facture.getLignes()) {
                    ligneStmt.setLong(1, facture.getId());
                    ligneStmt.setString(2, ligne.getReferenceArticle());
                    ligneStmt.setInt(3, ligne.getQuantite());
                    ligneStmt.setBigDecimal(4, ligne.getPrixUnitaire());
                    ligneStmt.addBatch();
                }
                ligneStmt.executeBatch();
            }

            conn.commit();
            System.out.println("Sauvegarde de " + factures.size() + " factures effectuée");

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw new RemoteException("Erreur lors de la sauvegarde des factures", e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }
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
    public BigDecimal calculerChiffreAffairesTotal(LocalDate date) throws RemoteException {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String sql = "SELECT SUM(total) as chiffre_total FROM factures WHERE date_facturation = ? AND statut = 'payee'";
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