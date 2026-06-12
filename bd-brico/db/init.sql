SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE DATABASE IF NOT EXISTS brico_merlin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE brico_merlin;

CREATE TABLE familles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE articles (
    ref VARCHAR(50) PRIMARY KEY,
    nom VARCHAR(200) NOT NULL,
    famille_id INT NOT NULL,
    prix_unitaire DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    FOREIGN KEY (famille_id) REFERENCES familles(id)
) ENGINE=InnoDB;

-- Données de test
INSERT INTO familles (nom) VALUES
    ('Visserie'),
    ('Peinture'),
    ('Outillage');

INSERT INTO articles VALUES
    ('ART001', 'Vis à bois 4x40',     1, 2.50,  100),
    ('ART002', 'Écrou M8',            1, 1.20,  50),
    ('ART003', 'Peinture blanche 1L', 2, 15.00, 20),
    ('ART004', 'Marteau 500g',        3, 49.99, 10),
    ('ART005', 'Peinture grise 1L',   2, 8.90,  35),
    ('ART006', 'Tournevis plat',      3, 12.50, 0);
