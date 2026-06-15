package org.example.server.magasin;

import org.example.common.model.Article;
import org.example.common.model.Client;
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
    public List<Client> getTousLesClients() throws RemoteException {
        List<Client> clients = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, nom_famille, prenom FROM clients ORDER BY nom_famille, prenom");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                clients.add(new Client(rs.getInt("id"), rs.getString("nom_famille"), rs.getString("prenom")));
            }
        } catch (SQLException e) {
            throw new RemoteException("Erreur lors de la récupération des clients", e);
        }
        return clients;
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
    public Facture passerEnCaisse(String nom, String prenom, Map<String, Integer> panier, String modePaiement)
            throws RemoteException {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            PreparedStatement findClient = conn.prepareStatement(
                    "SELECT id FROM clients WHERE nom_famille = ? AND prenom = ?");
            findClient.setString(1, nom);
            findClient.setString(2, prenom);
            ResultSet rsClient = findClient.executeQuery();

            int clientIntId;
            if (rsClient.next()) {
                clientIntId = rsClient.getInt("id");
            } else {
                PreparedStatement insertClient = conn.prepareStatement(
                        "INSERT INTO clients (nom_famille, prenom) VALUES (?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                insertClient.setString(1, nom);
                insertClient.setString(2, prenom);
                insertClient.executeUpdate();
                ResultSet keys = insertClient.getGeneratedKeys();
                keys.next();
                clientIntId = keys.getInt(1);
            }

            Facture facture = new Facture(String.valueOf(clientIntId), modePaiement);
            facture.setNomClient(nom);
            facture.setPrenomClient(prenom);
            facture.setPayee(true);

            for (Map.Entry<String, Integer> entry : panier.entrySet()) {
                String ref      = entry.getKey();
                int    quantite = entry.getValue();

                Article article = consulterStockArticle(ref);
                if (article == null || article.getQuantiteEnStock() < quantite) {
                    conn.rollback();
                    return null;
                }

                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE articles SET stock = stock - ? WHERE ref = ?");
                stmt.setInt(1, quantite);
                stmt.setString(2, ref);
                stmt.executeUpdate();

                facture.ajouterLigne(new LigneFacture(ref, article.getNom(), quantite, article.getPrixUnitaire()));
            }

            conn.commit();
            conn.setAutoCommit(true);
            factureStore.saveFacture(facture);
            System.out.println("Facture #" + facture.getId() + " créée — " + panier.size() + " article(s) — Client: " + nom + " " + prenom);
            return facture;

        } catch (SQLException e) {
            try {
                Connection conn = DatabaseConnection.getInstance().getConnection();
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {}
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
    public List<Facture> getFacturesClient(String nom, String prenom) throws RemoteException {
        return factureStore.readAllFactures().stream()
                .filter(f -> nom.equalsIgnoreCase(f.getNomClient()) && prenom.equalsIgnoreCase(f.getPrenomClient()))
                .toList();
    }

    @Override
    public List<Facture> getToutesLesFactures() throws RemoteException {
        return factureStore.readAllFactures();
    }


    @Override
    public BigDecimal calculerChiffreAffaires(LocalDate date) throws RemoteException {
        return factureStore.readAllFactures().stream()
                .filter(f -> f.isPayee() && date.equals(f.getDateFacturation()))
                .map(Facture::getTotalFacture)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}