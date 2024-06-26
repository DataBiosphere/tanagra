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
        pc.criteria_meta_seq as concept_id,
        regexp_extract(pc.name, '.*-(.*)') as name,
        regexp_extract(pc.name, '(.*)-.*') as concept_code
    FROM `${omopDataset}.icd10_criteria` pc
    WHERE not regexp_contains(pc.name, 'PCS') and pc.is_root = false
      and pc.is_leaf = true
) cc ON c.code = cc.concept_code

