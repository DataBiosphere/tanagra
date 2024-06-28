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
        cast(cast(pc.criteria_meta_seq as NUMERIC) as INT64) as concept_id,
        case when pc.name like '%EXPIRED%' then regexp_extract(pc.name, '.*-(\\[EX.*)')
             when pc.is_root = true then regexp_extract(pc.name, '.*[0-9] (.*)')
             when pc.is_root = false then regexp_extract(name, '.*[0-9]-(.*)')
        end as name,
        case when pc.name like '%EXPIRED%' then regexp_extract(pc.name, '(.*)-[\\(A-Z0-9].*')
             when pc.is_root = true then regexp_extract(pc.name, '(.*[0-9]) .*')
             when pc.is_root = false then regexp_extract(name, '(.*[0-9])-.*')
        end as concept_code,
        pc.is_leaf
    FROM `${omopDataset}.icd_criteria` pc
    ) cc ON c.code = cc.concept_code
    and cc.is_leaf = true
