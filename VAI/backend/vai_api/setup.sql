CREATE DATABASE IF NOT EXISTS vai_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE vai_db;

CREATE TABLE IF NOT EXISTS users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  semester INT NOT NULL DEFAULT 1,
  token VARCHAR(64) NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tasks (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  title VARCHAR(150) NOT NULL,
  due_date DATE NOT NULL,
  due_time TIME NOT NULL,
  category VARCHAR(20) NOT NULL DEFAULT 'Homework',
  priority VARCHAR(10) NOT NULL DEFAULT 'Mediana',
  reminder TINYINT(1) NOT NULL DEFAULT 1,
  done TINYINT(1) NOT NULL DEFAULT 0,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS items (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  title VARCHAR(150) NOT NULL,
  description TEXT,
  category VARCHAR(30) NOT NULL,
  price INT NOT NULL,
  cond VARCHAR(30) NOT NULL DEFAULT 'Buen estado',
  image VARCHAR(200) NULL,
  sold TINYINT(1) NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS swaps (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  title VARCHAR(150) NOT NULL,
  description TEXT,
  category VARCHAR(40) NOT NULL,
  wanted VARCHAR(200) NOT NULL,
  image VARCHAR(200) NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS messages (
  id INT AUTO_INCREMENT PRIMARY KEY,
  from_id INT NOT NULL,
  to_id INT NULL,
  room VARCHAR(40) NULL,
  subject VARCHAR(150) NULL,
  body TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (from_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Datos demo (usuario que no puede iniciar sesión) para que Campus Market no se vea vacío
INSERT IGNORE INTO users (id,name,email,password,semester) VALUES (1,'Juan S.','demo@ecci.edu.co','x',4);
INSERT INTO items (user_id,title,description,category,price,cond) VALUES
 (1,'Calculadora TI-84 Plus CE','Apenas se ha usado y la batería está en perfecto estado. Incluye cable cargador USB.','Electrónica',20000,'Excelente estado'),
 (1,'Sudadera con capucha','Sudadera universitaria morada talla M.','Vestir',50000,'Buen estado'),
 (1,'Kit de inicio Arduino','Kit completo con protoboard, sensores y cables.','Laboratorio',35000,'Como nuevo'),
 (1,'Juego de planos de ingeniería','Escuadras y reglas para dibujo técnico.','Suministros',15000,'Buen estado');
INSERT INTO swaps (user_id,title,description,category,wanted) VALUES
 (1,'Química orgánica','Solo tiene subrayados en el capítulo 1.','Libros','Libro de resolución de problemas matemáticos'),
 (1,'Protector de pantalla Paperlike para iPad','Paquete de 2 unidades sin abrir.','Otros','Libro de cálculo multivariable');
