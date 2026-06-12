package org.example.server.siege;

import org.example.common.model.Article;
import org.example.common.model.Facture;
import org.example.common.rmi.SiegeService;
import org.example.common.util.DatabaseConnection;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SiegeServiceImpl extends UnicastRemoteObject implements SiegeService {

    private static final String ARCHIVES_DIR = "archives";

    public SiegeServiceImpl() throws RemoteException {
        super();
        new File(ARCHIVES_DIR).mkdirs();
    }

    // =========================================================
    //  Archivage des factures dans un fichier texte daté
    // =========================================================

    @Override
    public void sauvegarderFactures(List<Facture> factures) throws RemoteException {
        if (factures == null || factures.isEmpty()) {
            System.out.println("Aucune facture à archiver.");
            return;
        }

        String archivePath = ARCHIVES_DIR + "/factures_" + LocalDate.now() + ".txt";

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(archivePath, true), StandardCharsets.UTF_8))) {

            String horodatage = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm:ss"));
            writer.write("=== ARCHIVAGE DU " + horodatage + " ===\n\n");
            for (Facture facture : factures) {
                writer.write(facture.toTicket());
            }

        } catch (IOException e) {
            throw new RemoteException("Erreur lors de l'archivage des factures", e);
        }

        System.out.println("Archivage de " + factures.size() + " factures dans " + archivePath);
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
    //  Articles & stock (lecture BD)
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

    @Override
    public BigDecimal calculerChiffreAffairesTotal(LocalDate date) throws RemoteException {
        // CA calculé à partir de l'archive du jour correspondant
        String archivePath = ARCHIVES_DIR + "/factures_" + date + ".txt";
        File archiveFile = new File(archivePath);
        if (!archiveFile.exists()) return BigDecimal.ZERO;

        BigDecimal total = BigDecimal.ZERO;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(archiveFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("TOTAL : ")) {
                    String valeur = line.substring("TOTAL : ".length()).replace("€", "").trim();
                    total = total.add(new BigDecimal(valeur));
                }
            }
        } catch (IOException | NumberFormatException e) {
            throw new RemoteException("Erreur lecture archive " + archivePath, e);
        }
        return total;
    }
}