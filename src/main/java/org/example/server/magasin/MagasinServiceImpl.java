package org.example.server.magasin;

import org.example.common.model.Article;
import org.example.common.model.Facture;
import org.example.common.model.Famille;
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
import java.util.Map;

public class MagasinServiceImpl extends UnicastRemoteObject implements MagasinService {

    private final FactureFileManager factureStore = new FactureFileManager();

    public MagasinServiceImpl() throws RemoteException {
        super();
    }

    // =========================================================
    //  Articles (stockés en BD)
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
    public Article consulterStockArticle(String reference) throws RemoteException {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String sql = "SELECT a.ref, a.nom, f.nom as famille_nom, a.prix_unitaire, a.stock " +
                         "FROM articles a LEFT JOIN familles f ON a.famille_id = f.id " +
                         "WHERE a.ref = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, reference);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Article(
                    rs.getString("ref"),
                    rs.getString("nom"),
                    rs.getString("famille_nom"),
                    rs.getBigDecimal("prix_unitaire"),
                    rs.getInt("stock")
                );
            }
            return null;
        } catch (SQLException e) {
            throw new RemoteException("Erreur lors de la consultation du stock", e);
        }
    }

    @Override
    public List<Famille> getToutesLesFamilles() throws RemoteException {
        List<Famille> familles = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT id, nom FROM familles ORDER BY nom");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                familles.add(new Famille(rs.getInt("id"), rs.getString("nom")));
            }
        } catch (SQLException e) {
            throw new RemoteException("Erreur lors de la récupération des familles", e);
        }
        return familles;
    }

    @Override
    public List<String> rechercherArticleParFamille(String famille) throws RemoteException {
        List<String> references = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String sql = "SELECT a.ref FROM articles a JOIN familles f ON a.famille_id = f.id " +
                         "WHERE f.nom = ? AND a.stock > 0";
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
    public boolean ajouterStock(String referenceArticle, int quantite) throws RemoteException {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String sql = "UPDATE articles SET stock = stock + ? WHERE ref = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, quantite);
            stmt.setString(2, referenceArticle);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RemoteException("Erreur lors de l'ajout de stock", e);
        }
    }

    @Override
    public void recevoirMiseAJourPrix(Map<String, BigDecimal> nouveauxPrix) throws RemoteException {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String sql = "UPDATE articles SET prix_unitaire = ? WHERE ref = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            for (Map.Entry<String, BigDecimal> entry : nouveauxPrix.entrySet()) {
                stmt.setBigDecimal(1, entry.getValue());
                stmt.setString(2, entry.getKey());
                stmt.addBatch();
            }
            stmt.executeBatch();
            System.out.println("Prix mis à jour pour " + nouveauxPrix.size() + " articles");
        } catch (SQLException e) {
            throw new RemoteException("Erreur lors de la mise à jour des prix", e);
        }
    }

    // =========================================================
    //  Factures (stockées dans factures.txt)
    // =========================================================

    @Override
    public boolean acheterArticle(String clientId, String referenceArticle, int quantite, String modePaiement)
            throws RemoteException {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            Article article = consulterStockArticle(referenceArticle);
            if (article == null || article.getQuantiteEnStock() < quantite) {
                conn.rollback();
                return false;
            }

            String sql = "UPDATE articles SET stock = stock - ? WHERE ref = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, quantite);
            stmt.setString(2, referenceArticle);
            stmt.executeUpdate();
            conn.commit();

            Facture facture = new Facture(clientId, modePaiement);
            facture.setPayee(true);
            facture.ajouterLigne(new LigneFacture(
                    referenceArticle, article.getNom(), quantite, article.getPrixUnitaire()));
            factureStore.saveFacture(facture);

            System.out.println("Facture #" + facture.getId() + " créée pour client " + clientId);
            return true;

        } catch (SQLException e) {
            throw new RemoteException("Erreur lors de l'achat", e);
        }
    }

    @Override
    public boolean passerEnCaisse(String clientId, Map<String, Integer> panier, String modePaiement)
            throws RemoteException {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            Facture facture = new Facture(clientId, modePaiement);
            facture.setPayee(true);

            for (Map.Entry<String, Integer> entry : panier.entrySet()) {
                String ref     = entry.getKey();
                int    quantite = entry.getValue();

                Article article = consulterStockArticle(ref);
                if (article == null || article.getQuantiteEnStock() < quantite) {
                    conn.rollback();
                    return false;
                }

                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE articles SET stock = stock - ? WHERE ref = ?");
                stmt.setInt(1, quantite);
                stmt.setString(2, ref);
                stmt.executeUpdate();

                facture.ajouterLigne(new LigneFacture(ref, article.getNom(), quantite, article.getPrixUnitaire()));
            }

            conn.commit();
            factureStore.saveFacture(facture);
            System.out.println("Facture #" + facture.getId() + " créée — " + panier.size() + " article(s)");
            return true;

        } catch (SQLException e) {
            throw new RemoteException("Erreur lors du passage en caisse", e);
        }
    }

    @Override
    public Facture consulterFacture(Long factureId) throws RemoteException {
        return factureStore.readAllFactures().stream()
                .filter(f -> f.getId().equals(factureId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean payerFacture(Long factureId, String modePaiement) throws RemoteException {
        return factureStore.updateStatut(factureId, true, modePaiement);
    }

    @Override
    public List<Facture> getFacturesClient(String clientId) throws RemoteException {
        return factureStore.readAllFactures().stream()
                .filter(f -> clientId.equals(f.getClientId()))
                .toList();
    }

    @Override
    public List<Facture> getToutesLesFactures() throws RemoteException {
        return factureStore.readAllFactures();
    }

    @Override
    public void viderFichierFactures() throws RemoteException {
        factureStore.vider();
        System.out.println("Fichier factures.txt vidé après archivage");
    }

    @Override
    public BigDecimal calculerChiffreAffaires(LocalDate date) throws RemoteException {
        return factureStore.readAllFactures().stream()
                .filter(f -> f.isPayee() && date.equals(f.getDateFacturation()))
                .map(Facture::getTotalFacture)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}