package org.example.server.magasin;

import org.example.common.model.Facture;
import org.example.common.rmi.MagasinService;
import org.example.common.rmi.SiegeService;

import java.math.BigDecimal;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MagasinServer {

    private static MagasinService magasinService;
    private static SiegeService   siegeService;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private static boolean modeDemo = false;

    public static void main(String[] args) {
        for (String arg : args) {
            if ("--demo".equalsIgnoreCase(arg)) {
                modeDemo = true;
                System.out.println("[DEMO] Mode démo activé : archivage dans 30s, sync prix dans 90s.");
            }
        }
        try {
            LocateRegistry.createRegistry(1099);
            magasinService = new MagasinServiceImpl();
            Naming.rebind("rmi://localhost:1099/MagasinService", magasinService);
            System.out.println("Serveur Magasin démarré sur rmi://localhost:1099/MagasinService");

            siegeService = connecterAuSiege();

            planifierSyncPrix();
            planifierArchivageFactures();

        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage du serveur magasin :");
            e.printStackTrace();
        }
    }

    private static SiegeService connecterAuSiege() {
        try {
            SiegeService service = (SiegeService) Naming.lookup("rmi://localhost:1098/SiegeService");
            System.out.println("Connecté au serveur Siège (rmi://localhost:1098/SiegeService)");
            return service;
        } catch (Exception e) {
            System.err.println("Impossible de se connecter au serveur Siège : " + e.getMessage());
            return null;
        }
    }

    private static long secondesJusqua(int heure, int minute) {
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime prochaine  = maintenant.toLocalDate().atTime(heure, minute);
        if (!prochaine.isAfter(maintenant)) {
            prochaine = prochaine.plusDays(1);
        }
        return java.time.Duration.between(maintenant, prochaine).getSeconds();
    }

    private static void planifierSyncPrix() {
        long delai = modeDemo ? 90 : secondesJusqua(7, 0);
        if (modeDemo) {
            System.out.println("[DEMO] Synchronisation des prix prévue dans 90 secondes.");
        } else {
            System.out.printf("Synchronisation des prix prévue dans %d h %d min (7h00)%n",
                    delai / 3600, (delai % 3600) / 60);
        }

        scheduler.schedule(() -> {
            try {
                if (siegeService == null) siegeService = connecterAuSiege();
                if (siegeService != null) {
                    System.out.println("\n[MAGASIN ← SIÈGE] Récupération des nouveaux prix" + (modeDemo ? " [DEMO]" : " (7h00)") + "...");
                    Map<String, BigDecimal> nouveauxPrix = siegeService.getNouveauxPrix();
                    magasinService.recevoirMiseAJourPrix(nouveauxPrix);
                    System.out.println("[MAGASIN ← SIÈGE] " + nouveauxPrix.size() + " prix synchronisés.");
                }
            } catch (Exception e) {
                System.err.println("Erreur synchronisation des prix : " + e.getMessage());
                siegeService = null;
            } finally {
                planifierSyncPrix();
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
                if (siegeService == null) siegeService = connecterAuSiege();
                if (siegeService != null) {
                    System.out.println("\n[MAGASIN → SIÈGE] Archivage des factures" + (modeDemo ? " [DEMO]" : " (22h00)") + "...");
                    List<Facture> factures = magasinService.getToutesLesFactures();
                    siegeService.sauvegarderFactures(factures);
                    System.out.println("[MAGASIN → SIÈGE] " + factures.size() + " facture(s) archivées.");
                }
            } catch (Exception e) {
                System.err.println("Erreur archivage des factures : " + e.getMessage());
                siegeService = null;
            } finally {
                planifierArchivageFactures();
            }
        }, delai, TimeUnit.SECONDS);
    }
}