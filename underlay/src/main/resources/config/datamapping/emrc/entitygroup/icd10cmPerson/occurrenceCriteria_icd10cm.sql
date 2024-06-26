SELECT c.IND_CODE_SEQ as icd10_occurrence_id,
       cc.concept_id AS icd10_id
FROM `${omopDataset}.icd10_codes` AS c
JOIN (
    SELECT
        pc.criteria_meta_seq as concept_id,
        regexp_extract(pc.name,r'(.*)-.*')  as concept_code
    FROM `${omopDataset}.icd10_criteria` pc
    WHERE not regexp_contains(pc.name, 'PCS') and pc.is_root = false
      and pc.is_leaf = true
) cc ON c.code = cc.concept_code

