INSERT INTO products (name, description, price, stock, active)
SELECT 'Notebook Pro 14', 'Rendimiento para tu día a día', 599990.00, 10, true
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Notebook Pro 14');

INSERT INTO products (name, description, price, stock, active)
SELECT 'Monitor UltraWide', 'Más espacio para crear', 329990.00, 15, true
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Monitor UltraWide');

INSERT INTO products (name, description, price, stock, active)
SELECT 'Teclado mecánico', 'Precisión en cada tecla', 89990.00, 25, true
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Teclado mecánico');

INSERT INTO products (name, description, price, stock, active)
SELECT 'Auriculares Studio', 'Sonido que te acompaña', 129990.00, 20, true
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Auriculares Studio');
