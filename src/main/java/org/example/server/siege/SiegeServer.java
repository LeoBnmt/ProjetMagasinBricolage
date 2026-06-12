package org.example.server.siege;

import org.example.common.model.Facture;
import org.example.common.rmi.MagasinService;
import org.example.common.rmi.SiegeService;

import java.math.BigDecimal;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SiegeServer {

    private static SiegeService siegeService;
    private static MagasinService magasinService;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public static void main(String[] args) {
        try {
            // Démarrer le registre RMI du siège sur le port 1098
            LocateRegistry.createRegistry(1098);
            siegeService = new SiegeServiceImpl();
            Naming.rebind("rmi://localhost:1098/SiegeService", siegeService);
            System.out.println("Serveur Siège démarré sur rmi://localhost:1098/SiegeService");

            // Se connecter au serveur Magasin via RMI (il doit être démarré avant)
            magasinService = connecterAuMagasin();

            // Lancer les tâches automatisées
            programmerTachesAutomatisees();

        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage du serveur siège :");
            e.printStackTrace();
        }
    }

    private static MagasinService connecterAuMagasin() {
        try {
            MagasinService service = (MagasinService) Naming.lookup("rmi://localhost:1099/MagasinService");
            System.out.println("Connecté au serveur Magasin (rmi://localhost:1099/MagasinService)");
            return service;
        } catch (Exception e) {
            System.err.println("Impossible de se connecter au serveur Magasin : " + e.getMessage());
            System.err.println("Les tâches automatisées seront ignorées jusqu'à reconnexion.");
            return null;
        }
    }

    private static void programmerTachesAutomatisees() {
        // Mise à jour des prix toutes les heures (simule la mise à jour du matin)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (magasinService == null) {
                    magasinService = connecterAuMagasin();
                    if (magasinService == null) return;
                }

                System.out.println("\n[SIÈGE → MAGASIN] Envoi des nouveaux prix...");
                Map<String, BigDecimal> nouveauxPrix = genererNouveauxPrix();

                // 1. Mettre à jour dans la BD du siège
                siegeService.mettreAJourPrix(nouveauxPrix);

                // 2. Pousser les nouveaux prix au serveur Magasin via RMI
                magasinService.recevoirMiseAJourPrix(nouveauxPrix);

                System.out.println("[SIÈGE → MAGASIN] " + nouveauxPrix.size() + " prix transmis avec succès.");

            } catch (Exception e) {
                System.err.println("Erreur lors de la mise à jour des prix : " + e.getMessage());
                magasinService = null; // forcer la reconnexion au prochain tour
            }
        }, 0, 60, TimeUnit.MINUTES);

        // Sauvegarde des factures toutes les 30 secondes (simule la sauvegarde du soir)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (magasinService == null) {
                    magasinService = connecterAuMagasin();
                    if (magasinService == null) return;
                }

                System.out.println("\n[MAGASIN → SIÈGE] Récupération des factures pour sauvegarde...");

                // 1. Récupérer toutes les factures du Magasin via RMI
                List<Facture> factures = magasinService.getToutesLesFactures();

                // 2. Les copier côté siège (archivage dans un fichier .txt daté)
                siegeService.sauvegarderFactures(factures);

                System.out.println("[MAGASIN → SIÈGE] " + factures.size() + " facture(s) archivées.");

            } catch (Exception e) {
                System.err.println("Erreur lors de la sauvegarde des factures : " + e.getMessage());
                magasinService = null; // forcer la reconnexion au prochain tour
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    private static Map<String, BigDecimal> genererNouveauxPrix() {
        // En production, ces prix viendraient d'une source externe (ERP, fichier, API...)
        Map<String, BigDecimal> prix = new HashMap<>();
        prix.put("ART001", new BigDecimal("2.60"));
        prix.put("ART002", new BigDecimal("1.25"));
        prix.put("ART003", new BigDecimal("15.50"));
        prix.put("ART004", new BigDecimal("51.99"));
        prix.put("ART005", new BigDecimal("9.00"));
        prix.put("ART006", new BigDecimal("13.00"));
        return prix;
    }
}
