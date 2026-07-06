-- Renumera orden por nivel (padres e hijas) para evitar duplicados históricos
WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY categoria_padre_id
               ORDER BY orden, id
           ) - 1 AS nuevo_orden
    FROM categorias
)
UPDATE categorias c
SET orden = ranked.nuevo_orden
FROM ranked
WHERE c.id = ranked.id;
