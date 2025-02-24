SELECT
    c.IND_CODE_SEQ as icd10_occurrence_id,
    c.IND_SEQ as person_id,
    cc.concept_id as icd10_concept_id,
    cc.name as icd10_concept_name,
    cc.concept_code as standard_code,
    CAST(FLOOR(cast(c.AGE_AT_EVENT as NUMERIC)) AS INT64) AS age_at_occurrence
FROM `${omopDataset}.icd10_codes` c
JOIN (
    SELECT
        concept_id,
        concept_name as name,
        vocabulary_id,
        concept_code,
        CASE WHEN concept_code IS NULL THEN concept_name ELSE CONCAT(concept_code, ' ', concept_name) END AS label
    FROM `${omopDataset}.concept`
    WHERE vocabulary_id = 'ICD10PCS'

    UNION ALL

    SELECT
        concept_id,
        concept_name as name,
        vocabulary_id,
        concept_code,
        CASE WHEN concept_code IS NULL THEN concept_name ELSE CONCAT(concept_code, ' ', concept_name) END AS label
    FROM `${staticTablesDataset}.prep_concept`
    WHERE vocabulary_id = 'ICD10PCS'
) cc ON c.code = cc.concept_code
