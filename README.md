# Projet Magasin Bricolage - Brico-Merlin

Système client-serveur RMI pour la gestion d'un magasin de bricolage avec JavaFX, MySQL et Docker.

## Architecture

- **Serveur Siège** (port 1098) : Gestion centralisée, mise à jour des prix, sauvegarde
- **Serveur Magasin** (port 1099) : Opérations de vente, stock, facturation
- **Clients JavaFX** : Interface graphique pour les points de vente
- **Base de données MySQL** : Stockage persistant des données

## Démarrage avec Docker

### 1. Démarrer les services backend
```bash
# Démarrer MySQL, Serveur Siège et Serveur Magasin
docker-compose up -d mysql siege-server magasin-server
```

### 2. Vérifier que les services sont démarrés
```bash
docker-compose logs -f
```

### 3. Lancer le client (en local)
```bash
# Option 1: Avec Maven
mvn javafx:run

# Option 2: Avec Docker (nécessite X11 forwarding sur Linux/Mac)
docker-compose --profile client up client
```

## Démarrage en local (sans Docker)

### 1. Démarrer MySQL
```bash
# Installer et démarrer MySQL, puis exécuter schema.sql
mysql -u root -p < src/main/resources/schema.sql
```

### 2. Démarrer les serveurs
```bash
# Terminal 1: Serveur Siège
mvn exec:java -Dexec.mainClass=org.example.server.headquarters.SiegeServer

# Terminal 2: Serveur Magasin
mvn exec:java -Dexec.mainClass=org.example.server.store.MagasinServer

# Terminal 3: Client JavaFX
mvn javafx:run
```

## Fonctionnalités Implémentées

### Opérations Magasin
- ✅ Consulter le stock d'un article
- ✅ Rechercher des articles par famille
- ✅ Acheter un article (création facture automatique)
- ✅ Payer une facture
- ✅ Consulter une facture
- ✅ Ajouter du stock
- ✅ Calculer le chiffre d'affaires d'une date

### Opérations Siège
- ✅ Mise à jour automatique des prix (tous les matins)
- ✅ Sauvegarde automatique des factures (tous les soirs)
- ✅ Synchronisation du stock
- ✅ Calcul du chiffre d'affaires total

### Interface JavaFX
- ✅ Gestion des articles avec tableaux
- ✅ Interface de vente intuitive
- ✅ Gestion des factures
- ✅ Statistiques et rapports

## Utilisation

### Interface Client
1. **Onglet Gestion Articles** : Consulter stock, rechercher par famille, ajouter stock
2. **Onglet Vente** : Effectuer des achats pour les clients
3. **Onglet Facturation** : Consulter et payer les factures
4. **Onglet Statistiques** : Calculer le chiffre d'affaires

### Exemples d'utilisation
- **Consulter un article** : Saisir "VIS001" dans Référence → Consulter Stock
- **Rechercher par famille** : Saisir "Visserie" dans Famille → Rechercher
- **Effectuer un achat** : Client "CLIENT001", Article "VIS001", Quantité "10" → Effectuer Achat
- **Consulter facture** : Saisir l'ID de facture → Consulter Facture

## Configuration Base de Données

Modifiez `src/main/java/org/example/common/util/DatabaseConnection.java` si nécessaire :
```java
private static final String URL = "jdbc:mysql://localhost:3306/brico_merlin";
private static final String USER = "root";
private static final String PASSWORD = "password";
```

## Arrêt des Services

```bash
# Arrêter tous les containers
docker-compose down

# Arrêter et supprimer les volumes
docker-compose down -v
```

## Données de Test

La base de données est initialisée avec des articles de test :
- VIS001, VIS002 (Visserie)
- MAR001, MAR002 (Marteaux)
- PER001, PER002 (Perceuses)
- PEI001, PEI002 (Peinture)

## Technologies Utilisées

- **Java 23** : Langage principal
- **JavaFX 23** : Interface graphique moderne
- **RMI** : Communication client-serveur
- **MySQL 8.0** : Base de données
- **Maven** : Gestion des dépendances
- **Docker** : Conteneurisation