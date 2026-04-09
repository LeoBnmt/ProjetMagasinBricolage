package org.example.server.store;

import org.example.common.rmi.MagasinService;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class MagasinServer {
    public static void main(String[] args) {
        try {
            // Créer le registre RMI sur le port 1099
            LocateRegistry.createRegistry(1099);

            // Créer l'instance du service
            MagasinService service = new MagasinServiceImpl();

            // Enregistrer le service dans le registre RMI
            Naming.rebind("rmi://localhost:1099/MagasinService", service);

            System.out.println("Serveur Magasin démarré et en attente de connexions...");
            System.out.println("Service disponible à: rmi://localhost:1099/MagasinService");

            // Maintenir le serveur en vie
            Object lock = new Object();
            synchronized (lock) {
                lock.wait();
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage du serveur magasin:");
            e.printStackTrace();
        }
    }
}