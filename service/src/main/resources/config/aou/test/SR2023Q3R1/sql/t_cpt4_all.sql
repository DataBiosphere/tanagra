SELECT
    pc.id,
    pc.concept_id,
    CASE WHEN c.concept_name IS NULL THEN pc.name ELSE c.concept_name END AS name,
    pc.type, pc.is_standard, pc.code,
    CASE WHEN pc.code IS NULL THEN pc.name ELSE CONCAT(pc.code, ' ', pc.name) END AS label,
    CASE WHEN c.domain_id is null THEN 'Unknown' ELSE c.domain_id END as domain_id

FROM `all-of-us-ehr-dev.SR2023Q3R1.prep_cpt` pc

LEFT JOIN `all-of-us-ehr-dev.SR2023Q3R1.concept` c
ON c.concept_id = pc.concept_id
AND c.vocabulary_id = pc.type

WHERE pc.type = 'CPT4'
