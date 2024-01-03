SELECT
    pc.id,
    pc.concept_id,
    CASE WHEN c.concept_name IS NULL THEN pc.name ELSE c.concept_name END AS name,
    pc.type,
    (CASE WHEN pc.is_standard = 0 THEN 'Source' ELSE 'Standard' END) AS standard_concept,
    pc.code as concept_code
    CASE WHEN pc.code IS NULL THEN pc.name ELSE CONCAT(pc.code, ' ', pc.name) END AS label

FROM `${staticTablesDataset}.prep_cpt` pc

LEFT JOIN `${omopDataset}.concept` c
    ON c.concept_id = pc.concept_id
    AND c.vocabulary_id = pc.type

WHERE pc.type = 'CPT4'
