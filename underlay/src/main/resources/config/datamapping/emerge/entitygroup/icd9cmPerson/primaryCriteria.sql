SELECT c.ind_seq as person_id,
       cc.concept_id AS icd9_id
FROM `${omopDataset}.icd_codes` AS c
JOIN (
    SELECT
        concept_id,
        concept_code
    FROM `${omopDataset}.concept`
    WHERE vocabulary_id = 'ICD9CM'

    UNION ALL

    SELECT
        concept_id,
        concept_code
    FROM `${staticTablesDataset}.prep_concept`
    WHERE vocabulary_id = 'ICD9CM'
) cc ON c.code = cc.concept_code
