SELECT
    pc.id,
    pc.concept_id,
    CASE WHEN c.concept_name IS NULL THEN pc.name ELSE c.concept_name END AS name,
    pc.type, pc.is_standard, pc.code,
    CASE WHEN pc.code IS NULL THEN pc.name ELSE CONCAT(pc.code, ' ', pc.name) END AS label

FROM `verily-tanagra-dev.aou_static_prep_useast1.prep_cpt` pc

LEFT JOIN `victr-tanagra-test.sd_20230328.concept` c
ON c.concept_id = pc.concept_id
AND c.vocabulary_id = pc.type

WHERE pc.type = 'CPT4'