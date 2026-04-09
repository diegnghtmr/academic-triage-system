-- V18: Coherencia de business rules — canon scalar de condition_value, FK a request_types, legacy seguro

-- 1) Limpiar FK errónea en reglas que no deben llevar tipo de solicitud
UPDATE business_rules
SET request_type_id = NULL
WHERE condition_type IN ('DEADLINE', 'IMPACT_LEVEL')
  AND request_type_id IS NOT NULL;

-- 2) REQUEST_TYPE: backfill determinístico por nombre de tipo (misma semántica que V7 seed)
UPDATE business_rules br
JOIN request_types rt ON rt.name = br.condition_value AND br.condition_type = 'REQUEST_TYPE'
SET br.request_type_id = rt.id,
    br.condition_value = CAST(rt.id AS CHAR)
WHERE br.active = TRUE
  AND br.condition_value REGEXP '[^0-9]'
  AND br.condition_value NOT LIKE '-%';

-- 3) REQUEST_TYPE: si ya es numérico pero falta FK y el id existe en catálogo
UPDATE business_rules br
SET br.request_type_id = CAST(br.condition_value AS UNSIGNED)
WHERE br.condition_type = 'REQUEST_TYPE'
  AND br.active = TRUE
  AND br.request_type_id IS NULL
  AND br.condition_value REGEXP '^[0-9]+$'
  AND EXISTS (SELECT 1 FROM request_types rt WHERE rt.id = CAST(br.condition_value AS UNSIGNED));

-- 4) Desactivar REQUEST_TYPE / REQUEST_TYPE_AND_DEADLINE activas sin tipo o valor no numérico (sin canon seguro)
UPDATE business_rules
SET active = FALSE
WHERE active = TRUE
  AND condition_type IN ('REQUEST_TYPE', 'REQUEST_TYPE_AND_DEADLINE')
  AND (
    request_type_id IS NULL
    OR condition_value NOT REGEXP '^[0-9]+$'
    OR CAST(condition_value AS SIGNED) < 0
  );

-- 5) Alinear condition_value con request_type_id cuando ambos existen en REQUEST_TYPE
UPDATE business_rules
SET condition_value = CAST(request_type_id AS CHAR)
WHERE condition_type = 'REQUEST_TYPE'
  AND active = TRUE
  AND request_type_id IS NOT NULL
  AND condition_value <> CAST(request_type_id AS CHAR);

-- 6) DEADLINE: desactivar umbrales no numéricos o negativos
UPDATE business_rules
SET active = FALSE
WHERE condition_type = 'DEADLINE'
  AND active = TRUE
  AND (
    condition_value NOT REGEXP '^[0-9]+$'
    OR CAST(condition_value AS SIGNED) < 0
  );

-- 7) IMPACT_LEVEL: normalizar mayúsculas; desactivar valores fuera de canon
UPDATE business_rules
SET condition_value = UPPER(condition_value)
WHERE condition_type = 'IMPACT_LEVEL'
  AND active = TRUE;

UPDATE business_rules
SET active = FALSE
WHERE condition_type = 'IMPACT_LEVEL'
  AND active = TRUE
  AND UPPER(condition_value) NOT IN ('HIGH', 'MEDIUM', 'LOW');
