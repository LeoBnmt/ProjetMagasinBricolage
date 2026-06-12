package org.example.server.magasin;

import org.example.common.model.Facture;
import org.example.common.model.LigneFacture;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class FactureFileManager {

    private static final String FILE_PATH = "factures.txt";
    private final AtomicLong nextId = new AtomicLong(1);

    public FactureFileManager() {
        List<Facture> existantes = readAllFactures();
        long maxId = existantes.stream().mapToLong(Facture::getId).max().orElse(0L);
        nextId.set(maxId + 1);
    }

    public synchronized long saveFacture(Facture facture) {
        long id = nextId.getAndIncrement();
        facture.setId(id);
        try (BufferedWriter w = openWriter(true)) {
            w.write(facture.toTicket());
        } catch (IOException e) {
            throw new RuntimeException("Erreur écriture facture dans " + FILE_PATH, e);
        }
        return id;
    }

    public synchronized List<Facture> readAllFactures() {
        List<Facture> factures = new ArrayList<>();
        File file = new File(FILE_PATH);
        if (!file.exists()) return factures;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            Facture current = null;
            boolean inLignes = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("FACTURE #")) {
                    current = new Facture();
                    current.setId(Long.parseLong(line.substring("FACTURE #".length())));
                    inLignes = false;

                } else if (current == null) {
                    continue;

                } else if (line.startsWith("Date : ")) {
                    current.setDateFacturation(LocalDate.parse(line.substring("Date : ".length())));

                } else if (line.startsWith("Client : ")) {
                    current.setClientId(line.substring("Client : ".length()));

                } else if (line.startsWith("Mode : ")) {
                    current.setModePaiement(line.substring("Mode : ".length()));

                } else if (line.startsWith("Statut : ")) {
                    current.setPayee("PAYEE".equals(line.substring("Statut : ".length())));

                } else if (line.equals(Facture.LINE_SEP)) {
                    inLignes = !inLignes;

                } else if (line.equals(Facture.SEPARATOR)) {
                    factures.add(current);
                    current = null;
                    inLignes = false;

                } else if (inLignes && !line.startsWith("TOTAL")) {
                    // Format : "ART001     | x2   |     2.50€ |     5.00€"
                    String[] parts = line.split(" \\| ");
                    if (parts.length == 4) {
                        String ref   = parts[0].trim();
                        int    qte   = Integer.parseInt(parts[1].trim().substring(1).trim());
                        BigDecimal prix = new BigDecimal(parts[2].trim().replace("€", ""));
                        current.ajouterLigne(new LigneFacture(ref, ref, qte, prix));
                    }
                }
            }
            if (current != null) factures.add(current);

        } catch (IOException e) {
            throw new RuntimeException("Erreur lecture " + FILE_PATH, e);
        }
        return factures;
    }

    public synchronized boolean updateStatut(Long id, boolean payee, String modePaiement) {
        List<Facture> factures = readAllFactures();
        boolean found = false;
        for (Facture f : factures) {
            if (f.getId().equals(id)) {
                f.setPayee(payee);
                f.setModePaiement(modePaiement);
                found = true;
                break;
            }
        }
        if (found) writeAll(factures);
        return found;
    }

    public synchronized void writeAll(List<Facture> factures) {
        try (BufferedWriter w = openWriter(false)) {
            for (Facture f : factures) {
                w.write(f.toTicket());
            }
        } catch (IOException e) {
            throw new RuntimeException("Erreur réécriture " + FILE_PATH, e);
        }
    }

    public synchronized void vider() {
        try (BufferedWriter w = openWriter(false)) {
            // fichier vidé intentionnellement après archivage
        } catch (IOException e) {
            throw new RuntimeException("Erreur vidage " + FILE_PATH, e);
        }
        nextId.set(1);
    }

    private BufferedWriter openWriter(boolean append) throws IOException {
        return new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(FILE_PATH, append), StandardCharsets.UTF_8));
    }
}