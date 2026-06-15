package org.example.server.siege;

import org.example.common.model.Facture;
import org.example.common.rmi.MagasinService;
import org.example.common.rmi.SiegeService;

import java.math.BigDecimal;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.time.LocalDateTime;
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
    private static boolean modeDemo = false;

    public static void main(String[] args) {
        for (String arg : args) {
            if ("--demo".equalsIgnoreCase(arg)) {
                modeDemo = true;
                System.out.println("[DEMO] Mode démo activé : archivage dans 30s, prix dans 60s.");
            }
        }
        try {
            LocateRegistry.createRegistry(1098);
            siegeService = new SiegeServiceImpl();
            Naming.rebind("rmi://localhost:1098/SiegeService", siegeService);
            System.out.println("Serveur Siège démarré sur rmi://localhost:1098/SiegeService");

            magasinService = connecterAuMagasin();

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
            return null;
        }
    }

    /**
     * Calcule le nombre de secondes jusqu'au prochain déclenchement à l'heure indiquée.
     */
    private static long secondesJusqua(int heure, int minute) {
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime prochaine  = maintenant.toLocalDate().atTime(heure, minute);
        if (!prochaine.isAfter(maintenant)) {
            prochaine = prochaine.plusDays(1);
        }
        return java.time.Duration.between(maintenant, prochaine).getSeconds();
    }

    private static void programmerTachesAutomatisees() {
        planifierMiseAJourPrix();
        planifierArchivageFactures();
    }

    private static void planifierMiseAJourPrix() {
        long delai = modeDemo ? 90 : secondesJusqua(7, 0);
        if (modeDemo) {
            System.out.println("[DEMO] Mise à jour des prix prévue dans 90 secondes.");
        } else {
            System.out.printf("Mise à jour des prix prévue dans %d h %d min (7h00)%n",
                    delai / 3600, (delai % 3600) / 60);
        }

        scheduler.schedule(() -> {
            try {
                if (magasinService == null) magasinService = connecterAuMagasin();
                if (magasinService != null) {
                    System.out.println("\n[SIÈGE → MAGASIN] Mise à jour des prix (7h00)...");
                    Map<String, BigDecimal> nouveauxPrix = genererNouveauxPrix();
                    siegeService.mettreAJourPrix(nouveauxPrix);
                    magasinService.recevoirMiseAJourPrix(nouveauxPrix);
                    System.out.println("[SIÈGE → MAGASIN] " + nouveauxPrix.size() + " prix transmis.");
                }
            } catch (Exception e) {
                System.err.println("Erreur mise à jour des prix : " + e.getMessage());
                magasinService = null;
            } finally {
                planifierMiseAJourPrix(); // replanifier pour le lendemain à 7h
            }
        }, delai, TimeUnit.SECONDS);
    }

    private static void planifierArchivageFactures() {
        long delai = modeDemo ? 30 : secondesJusqua(22, 0);
        if (modeDemo) {
            System.out.println("[DEMO] Archivage des factures prévu dans 30 secondes.");
        } else {
            System.out.printf("Archivage des factures prévu dans %d h %d min (22h00)%n",
                    delai / 3600, (delai % 3600) / 60);
        }

        scheduler.schedule(() -> {
            try {
                if (magasinService == null) magasinService = connecterAuMagasin();
                if (magasinService != null) {
                    System.out.println("\n[MAGASIN → SIÈGE] Archivage des factures (22h00)...");
                    List<Facture> factures = magasinService.getToutesLesFactures();
                    siegeService.sauvegarderFactures(factures);
                    System.out.println("[MAGASIN → SIÈGE] " + factures.size() + " facture(s) archivées.");
                }
            } catch (Exception e) {
                System.err.println("Erreur archivage des factures : " + e.getMessage());
                magasinService = null;
            } finally {
                planifierArchivageFactures(); // replanifier pour le lendemain à 22h
            }
        }, delai, TimeUnit.SECONDS);
    }

    private static Map<String, BigDecimal> genererNouveauxPrix() {
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