SELECT c.ind_seq as person_id,
       cc.concept_id AS icd10_id
FROM `${omopDataset}.icd10_codes` AS c
JOIN (
    SELECT
        concept_id,
        vocabulary_id,
        concept_code,
    FROM `${omopDataset}.concept`
    WHERE
        vocabulary_id = 'ICD10CM'
        AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0

    UNION ALL

    SELECT
        concept_id,
        vocabulary_id,
        concept_code,
    FROM `${staticTablesDataset}.prep_concept`
    WHERE
        vocabulary_id = 'ICD10CM'
        AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0
) cc ON c.code = cc.concept_code
WHERE cc.concept_id NOT IN (SELECT parent FROM `vumc-emerge-dev.indexed_chase_emerge_test.HCP_icd10cm_default`)
