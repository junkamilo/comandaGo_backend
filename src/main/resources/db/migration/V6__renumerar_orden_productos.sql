-- Renumera orden por categoría para evitar duplicados históricos
WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY categoria_id
               ORDER BY orden, id
           ) - 1 AS nuevo_orden
    FROM productos
)
UPDATE productos p
SET orden = ranked.nuevo_orden
FROM ranked
WHERE p.id = ranked.id;
