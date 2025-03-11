SELECT
    concept_id,
    concept_name,
    vocabulary_id,
    (CASE WHEN standard_concept IS NULL THEN 'Source' WHEN standard_concept = 'S' THEN 'Standard' ELSE 'Unknown' END) AS standard_concept,
    concept_code,
    CASE WHEN concept_code IS NULL THEN concept_name ELSE CONCAT(concept_code, ' ', concept_name) END AS label
FROM `${omopDataset}.concept`
WHERE
        vocabulary_id = 'ICD10PCS'

UNION ALL

SELECT
    concept_id,
    concept_name,
    vocabulary_id,
    (CASE WHEN standard_concept IS NULL THEN 'Source' WHEN standard_concept = 'S' THEN 'Standard' ELSE 'Unknown' END) AS standard_concept,
    concept_code,
    CASE WHEN concept_code IS NULL THEN concept_name ELSE CONCAT(concept_code, ' ', concept_name) END AS label
FROM `${staticTablesDataset}.prep_concept`
WHERE
        vocabulary_id = 'ICD10PCS'
