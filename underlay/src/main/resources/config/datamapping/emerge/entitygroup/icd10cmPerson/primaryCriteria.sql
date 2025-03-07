SELECT c.ind_seq as person_id,
       cc.concept_id AS icd10_id
FROM `${omopDataset}.icd10_codes` AS c
JOIN (
    SELECT
        concept_id,
        vocabulary_id,
        concept_code,
    FROM `${omopDataset}.concept`
    WHERE vocabulary_id = 'ICD10CM'

    UNION ALL

    SELECT
        concept_id,
        vocabulary_id,
        concept_code
    FROM `${staticTablesDataset}.prep_concept`
    WHERE vocabulary_id = 'ICD10CM'
) cc ON c.code = cc.concept_code
