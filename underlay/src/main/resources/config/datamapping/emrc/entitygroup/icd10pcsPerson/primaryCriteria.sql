SELECT c.ind_seq as person_id,
       cc.concept_id AS icd10_id
FROM `${omopDataset}.icd10_codes` AS c
JOIN (
    SELECT
        pc.criteria_meta_seq as concept_id,
        regexp_extract(pc.name, '(.*)-.*') as concept_code,
        pc.is_leaf
    FROM `${omopDataset}.icd10_criteria` pc
    WHERE pc.parent_seq >= (
        select criteria_meta_seq from `${omopDataset}.icd10_criteria`
        where starts_with(label, 'ICD10PCS')
        )
    ) cc ON c.code = cc.concept_code
