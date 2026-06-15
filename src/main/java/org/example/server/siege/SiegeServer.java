package org.example.server.siege;

import org.example.common.rmi.SiegeService;

import java.math.BigDecimal;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SiegeServer {

    private static SiegeService siegeService;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static boolean modeDemo = false;

    public static void main(String[] args) {
        for (String arg : args) {
            if ("--demo".equalsIgnoreCase(arg)) {
                modeDemo = true;
                System.out.println("[DEMO] Mode démo activé : mise à jour des prix dans 60s.");
            }
        }
        try {
            LocateRegistry.createRegistry(1098);
            siegeService = new SiegeServiceImpl();
            Naming.rebind("rmi://localhost:1098/SiegeService", siegeService);
            System.out.println("Serveur Siège démarré sur rmi://localhost:1098/SiegeService");
            System.out.println("En attente des connexions des magasins...");

            planifierMiseAJourPrix();

        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage du serveur siège :");
            e.printStackTrace();
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

    private static void planifierMiseAJourPrix() {
        long delai = modeDemo ? 60 : secondesJusqua(7, 0);
        if (modeDemo) {
            System.out.println("[DEMO] Mise à jour des prix prévue dans 60 secondes.");
        } else {
            System.out.printf("Mise à jour des prix prévue dans %d h %d min (7h00)%n",
                    delai / 3600, (delai % 3600) / 60);
        }

        scheduler.schedule(() -> {
            try {
                System.out.println("\n[SIÈGE] Mise à jour des prix" + (modeDemo ? " [DEMO]" : " (7h00)") + "...");
                Map<String, BigDecimal> nouveauxPrix = genererNouveauxPrix();
                siegeService.mettreAJourPrix(nouveauxPrix);
                System.out.println("[SIÈGE] " + nouveauxPrix.size() + " prix mis à jour en BD. Les magasins peuvent les récupérer.");
            } catch (Exception e) {
                System.err.println("Erreur mise à jour des prix : " + e.getMessage());
            } finally {
                planifierMiseAJourPrix();
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