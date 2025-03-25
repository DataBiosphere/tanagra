SELECT c.ind_seq as person_id,
       cc.concept_id AS icd9_id
FROM `${omopDataset}.icd_codes` AS c
JOIN (
    SELECT
        concept_id,
        concept_code
    FROM `${omopDataset}.concept`
    WHERE vocabulary_id = 'ICD9Proc'

    UNION ALL

    SELECT
        concept_id,
        concept_code
    FROM `${staticTablesDataset}.prep_concept`
    WHERE vocabulary_id = 'ICD9Proc'
) cc ON c.code = cc.concept_code
