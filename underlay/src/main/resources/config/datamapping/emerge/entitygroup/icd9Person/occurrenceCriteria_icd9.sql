SELECT c.IND_CODE_SEQ as icd9_occurrence_id,
       cc.concept_id AS icd9_id
FROM `${omopDataset}.icd_codes` AS c
JOIN (
    SELECT
        cast(cast(pc.criteria_meta_seq as NUMERIC) as INT64) as concept_id,
        case when pc.name like '%EXPIRED%' then regexp_extract(pc.name, '(.*)-[\\(A-Z0-9].*')
             when pc.is_root = true then regexp_extract(pc.name, '(.*[0-9]) .*')
             when pc.is_root = false then regexp_extract(name, '(.*[0-9])-.*')
            end as concept_code
    FROM `${omopDataset}.icd_criteria` pc
    ) cc ON c.code = cc.concept_code

