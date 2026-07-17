-- Corrige recetas existentes: quita marca Extra sin borrar la receta.
UPDATE receta_ingredientes
SET es_extra = FALSE,
    precio_extra = NULL
WHERE es_extra = TRUE
   OR precio_extra IS NOT NULL;

ALTER TABLE receta_ingredientes DROP CONSTRAINT IF EXISTS chk_receta_extra_precio;

ALTER TABLE receta_ingredientes DROP COLUMN IF EXISTS precio_extra;
ALTER TABLE receta_ingredientes DROP COLUMN IF EXISTS es_extra;
