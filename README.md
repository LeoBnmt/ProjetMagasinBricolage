# Projet Magasin Bricolage - Brico-Merlin

Système client-serveur RMI pour la gestion d'un magasin de bricolage avec JavaFX, MySQL et Docker.

## Architecture

- **Serveur Siège** (port RMI 1098) : mise à jour des prix, sauvegarde des factures, chiffre d'affaires global
- **Serveur Magasin** (port RMI 1099) : opérations de vente, stock, facturation
- **Client JavaFX** : interface graphique pour les points de vente
- **Base de données MySQL** (Docker, port 3307) : stockage persistant

## Démarrage

L'ordre est important : BD → Magasin → Siège → Client (le Siège se connecte au Magasin au démarrage).

### 1. Démarrer MySQL (Docker)
```bash
docker-compose -f bd-brico/docker-compose.yml up -d
```

### 2. Démarrer les serveurs
```bash
# Terminal 1 : Serveur Magasin
mvn exec:java -Dexec.mainClass=org.example.server.magasin.MagasinServer

# Terminal 2 : Serveur Siège
mvn exec:java -Dexec.mainClass=org.example.server.siege.SiegeServer

# Terminal 3 : Client JavaFX
mvn javafx:run
```

## Tests

```bash
mvn test
```

## Fonctionnalités

### Opérations Magasin
- Consulter le stock d'un article
- Rechercher des articles par famille (stock non nul)
- Acheter un article (création facture automatique)
- Payer une facture
- Consulter une facture
- Ajouter du stock (référence existante)
- Calculer le chiffre d'affaires à une date

### Opérations Siège (automatiques)
- Mise à jour des prix poussée au Magasin via RMI (le matin)
- Sauvegarde des factures du Magasin via RMI (le soir)

### Interface Client
1. **Articles** : consulter stock, rechercher par famille, ajouter stock
2. **Vente** : effectuer des achats pour les clients
3. **Facturation** : consulter et payer les factures
4. **Statistiques** : calculer le chiffre d'affaires

### Exemples d'utilisation
- **Consulter un article** : sélectionner "ART001" → Consulter Stock
- **Rechercher par famille** : sélectionner "Visserie" → Rechercher
- **Effectuer un achat** : Client "CLIENT001", Article "ART001", Quantité "10" → Effectuer Achat
- **Consulter facture** : saisir l'ID de facture → Consulter Facture

## Base de données

Credentials : `root / root`, base `brico_merlin`, port **3307** (configuré dans
`src/main/java/org/example/common/util/DatabaseConnection.java`).

Schéma et données de test : `bd-brico/db/init.sql`
Tables : `familles`, `articles`, `factures`, `lignes_facture`

### Réinitialiser la base

Le script `init.sql` ne s'exécute qu'à la première création du volume Docker. Pour repartir d'une base propre :
```bash
docker exec -i brico-merlin-db mysql -uroot -proot -e "DROP DATABASE brico_merlin;"
docker exec -i brico-merlin-db mysql -uroot -proot < bd-brico/db/init.sql
```

### Données de test

Articles `ART001` à `ART006`, familles : Visserie, Peinture, Outillage.

## Arrêt des services

```bash
# Arrêter le conteneur MySQL
docker-compose -f bd-brico/docker-compose.yml down

# Arrêter et supprimer le volume (perte des données)
docker-compose -f bd-brico/docker-compose.yml down -v
```

## Technologies

- **Java 17** : langage principal
- **JavaFX 22** : interface graphique
- **RMI** : middleware client-serveur
- **MySQL 8.0** : base de données
- **Maven** : build et dépendances
- **JUnit 5 / Mockito** : tests
- **Docker** : conteneurisation de la BD
