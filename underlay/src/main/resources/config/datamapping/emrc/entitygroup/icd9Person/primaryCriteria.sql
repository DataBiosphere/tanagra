SELECT c.ind_seq as person_id,
       cc.concept_id AS icd9_id
FROM `${omopDataset}.icd_codes` AS c
JOIN (
    SELECT
        cast(cast(pc.criteria_meta_seq as NUMERIC) as INT64) as concept_id,
        regexp_replace(pc.name, r'-.*', '') as concept_code,
        pc.is_leaf
    FROM `${omopDataset}.icd_criteria` pc
    WHERE name like '%EXPIRED%'
      and pc.is_root=false
    UNION ALL
    SELECT
        cast(cast(pc.criteria_meta_seq as NUMERIC) as INT64) as concept_id,
        regexp_replace(pc.name, r'-.*', '') as concept_code,
        pc.is_leaf
    FROM `${omopDataset}.icd_criteria` pc
    WHERE pc.name not like '%EXPIRED%'
      and pc.is_root=false
) cc ON c.code = cc.concept_code
     and cc.is_leaf = true
