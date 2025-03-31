SELECT
    c.IND_CODE_SEQ as icd9_occurrence_id,
    c.IND_SEQ as person_id,
    cc.concept_id as icd9_concept_id,
    cc.name as icd9_concept_name,
    cc.concept_code as standard_code,
    CAST(FLOOR(cast(c.AGE_AT_EVENT as NUMERIC)) AS INT64) AS age_at_occurrence
FROM `${omopDataset}.icd_codes` c
JOIN (
    SELECT
        concept_id,
        concept_name as name,
        concept_code
    FROM `${omopDataset}.concept`
    WHERE vocabulary_id = 'ICD9CM'
) cc ON c.code = cc.concept_code
