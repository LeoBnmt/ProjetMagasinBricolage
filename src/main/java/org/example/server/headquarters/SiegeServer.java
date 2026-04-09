package org.example.server.headquarters;

import org.example.common.rmi.SiegeService;

import java.math.BigDecimal;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SiegeServer {
    private static SiegeService service;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public static void main(String[] args) {
        try {
            // Créer le registre RMI sur le port 1098
            LocateRegistry.createRegistry(1098);

            // Créer l'instance du service
            service = new SiegeServiceImpl();

            // Enregistrer le service dans le registre RMI
            Naming.rebind("rmi://localhost:1098/SiegeService", service);

            System.out.println("Serveur Siège démarré et en attente de connexions...");
            System.out.println("Service disponible à: rmi://localhost:1098/SiegeService");

            // Programmer les tâches automatisées
            programmerTachesAutomatisees();

        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage du serveur siège:");
            e.printStackTrace();
        }
    }

    private static void programmerTachesAutomatisees() {
        // Mise à jour des prix tous les matins à 8h (simulé toutes les minutes pour le test)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("Mise à jour automatique des prix...");
                Map<String, BigDecimal> nouveauPrix = genererNouveauxPrix();
                service.mettreAJourPrix(nouveauPrix);
                System.out.println("Mise à jour des prix terminée");
            } catch (Exception e) {
                System.err.println("Erreur lors de la mise à jour automatique des prix: " + e.getMessage());
            }
        }, 1, 60, TimeUnit.MINUTES);

        // Sauvegarde des factures tous les soirs à 22h (simulé toutes les 30 secondes pour le test)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("Sauvegarde automatique des factures...");
                // En réalité, il faudrait récupérer les factures des magasins via RMI
                System.out.println("Sauvegarde des factures terminée");
            } catch (Exception e) {
                System.err.println("Erreur lors de la sauvegarde automatique: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    private static Map<String, BigDecimal> genererNouveauxPrix() {
        // Simulation de nouveaux prix (en réalité, ils viendraient d'une source externe)
        Map<String, BigDecimal> nouveauPrix = new HashMap<>();
        nouveauPrix.put("VIS001", new BigDecimal("0.16"));
        nouveauPrix.put("VIS002", new BigDecimal("0.26"));
        nouveauPrix.put("MAR001", new BigDecimal("16.50"));
        nouveauPrix.put("MAR002", new BigDecimal("26.00"));
        return nouveauPrix;
    }
}