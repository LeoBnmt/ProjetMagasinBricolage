# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Projet

**Brico-Merlin** — Application de gestion de magasin de bricolage basée sur une architecture distribuée Java RMI avec interface JavaFX.

## Commandes essentielles

### Build
```bash
mvn clean compile
mvn package
```

### Démarrer la base de données (Docker)
```bash
docker-compose -f bd-brico/docker-compose.yml up -d
```

### Lancer les serveurs (ordre important)
```bash
# 1. Serveur Siège (port 1098)
mvn exec:java -Dexec.mainClass=org.example.server.siege.SiegeServer

# 2. Serveur Magasin (port 1099)
mvn exec:java -Dexec.mainClass=org.example.server.magasin.MagasinServer

# 3. Client JavaFX
mvn javafx:run
```

### Tests
```bash
mvn test
mvn test -Dtest=NomDeLaClasse    # test unique
```

## Architecture

Architecture **Client-Serveur distribuée à deux niveaux** via **Java RMI**.

### Composants

| Composant | Port RMI | Classe principale | Rôle |
|-----------|----------|-------------------|------|
| Serveur Siège | 1098 | `SiegeServer` / `SiegeServiceImpl` | Siège social — mises à jour de prix, archivage de factures, synchronisation des stocks |
| Serveur Magasin | 1099 | `MagasinServer` / `MagasinServiceImpl` | Opérations magasin — ventes, stocks, factures |
| Client JavaFX | — | `ClientApplication` / `ClientController` | Interface graphique FXML, se connecte à `rmi://localhost:1099/MagasinService` |

### Flux de communication
```
ClientJavaFX → RMI → MagasinServer → JDBC → MySQL (port 3307)
                           ↑
                      SiegeServer (tâches planifiées : prix à 8h, archivage à 22h)
```

### Interfaces RMI (contrats)
- `MagasinService` — vérifier stock, rechercher par famille, traiter achats, payer factures, gérer stock
- `SiegeService` — mettre à jour les prix, archiver les factures, calculer le CA total

### Modèles de données (tous `Serializable` pour RMI)
- `Article` — ref, famille, prix unitaire, stock
- `Facture` — id, client_id, total, lignes, mode paiement, date, statut payé
- `LigneFacture` — ref article, désignation, quantité, prix unitaire, sous-total

## Base de données

- **MySQL 8.0** sur le port **3307** (Docker) ou 3306 (local)
- Credentials : `root / root`, base : `brico_merlin`
- Schéma et données de test : `bd-brico/db/init.sql`
- Connexion via singleton : `src/main/java/org/example/common/util/DatabaseConnection.java`

Tables : `familles`, `articles`, `factures`, `lignes_facture`

## Points clés

- Les opérations d'achat utilisent des **transactions JDBC** (BEGIN/COMMIT/ROLLBACK) pour la cohérence
- Le `SiegeServer` utilise `ScheduledExecutorService` pour les tâches automatiques
- L'UI JavaFX est définie dans `src/main/resources/fxml/client-view.fxml` (4 onglets : Articles, Ventes, Factures, Statistiques)
- Les serveurs doivent être démarrés **avant** le client (le registre RMI est créé par les serveurs)