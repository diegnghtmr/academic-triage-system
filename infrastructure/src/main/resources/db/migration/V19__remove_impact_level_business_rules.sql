-- V19: Retiro total de reglas IMPACT_LEVEL (semántica retirada sin reemplazo en contrato ni runtime)
DELETE FROM business_rules WHERE condition_type = 'IMPACT_LEVEL';
