CREATE DATABASE IF NOT EXISTS brico_merlin;
USE brico_merlin;

CREATE TABLE familles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE articles (
    ref VARCHAR(50) PRIMARY KEY,
    famille_id INT NOT NULL,
    prix_unitaire DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    FOREIGN KEY (famille_id) REFERENCES familles(id)
) ENGINE=InnoDB;

CREATE TABLE factures (
    id INT AUTO_INCREMENT PRIMARY KEY,
    client_id VARCHAR(50) NOT NULL,
    date_facturation DATE NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    mode_paiement ENUM('carte', 'especes', 'cheque') NOT NULL,
    statut ENUM('en_attente', 'payee') DEFAULT 'en_attente'
) ENGINE=InnoDB;

CREATE TABLE lignes_facture (
    id INT AUTO_INCREMENT PRIMARY KEY,
    facture_id INT NOT NULL,
    ref_article VARCHAR(50) NOT NULL,
    quantite INT NOT NULL,
    prix_unitaire DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (facture_id) REFERENCES factures(id),
    FOREIGN KEY (ref_article) REFERENCES articles(ref)
) ENGINE=InnoDB;

-- Données de test
INSERT INTO familles (nom) VALUES
    ('Visserie'),
    ('Peinture'),
    ('Outillage');

INSERT INTO articles VALUES
    ('ART001', 1, 2.50,  100),
    ('ART002', 1, 1.20,  50),
    ('ART003', 2, 15.00, 20),
    ('ART004', 3, 49.99, 10),
    ('ART005', 2, 8.90,  35),
    ('ART006', 3, 12.50, 0);
