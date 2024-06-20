SELECT
    cast(cast(pc.criteria_meta_seq as NUMERIC) as INT64) as id,
    cast(cast(pc.criteria_meta_seq as NUMERIC) as INT64) as concept_id,
    regexp_extract(pc.name,r'.*-(.EX.*)') as name,
    pc.type,
    'Source' as is_standard,
    regexp_replace(pc.name, r'-.*', '') as concept_code,
    concat(regexp_replace(pc.name, r'-.*', ''),' ',regexp_extract(pc.name,r'.*-(.EX.*)')) as label
FROM `${omopDataset}.icd_criteria` pc
WHERE name like '%EXPIRED%'
UNION ALL
SELECT
    cast(cast(pc.criteria_meta_seq as NUMERIC) as INT64) as id,
    cast(cast(pc.criteria_meta_seq as NUMERIC) as INT64) as concept_id,
    regexp_extract(pc.name, '.* ([A-Z].*)') as name,
    pc.type,
    'Source' as is_standard,
    regexp_extract(pc.name, '(.*) [A-Z].*') as concept_code,
    concat(regexp_extract(pc.name, '(.*) [A-Z].*'),' ',regexp_extract(pc.name, '.* ([A-Z].*)')) as label
FROM `${omopDataset}.icd_criteria` pc
WHERE pc.name not like '%EXPIRED%'
  and pc.is_root=true
UNION ALL
SELECT
    cast(cast(pc.criteria_meta_seq as NUMERIC) as INT64) as id,
    cast(cast(pc.criteria_meta_seq as NUMERIC) as INT64) as concept_id,
    regexp_extract(pc.name,r'.*-(.*)') as name,
    pc.type,
    'Source' as is_standard,
    regexp_replace(pc.name, r'-.*', '') as concept_code,
    concat(regexp_replace(pc.name, r'-.*', ''),' ',regexp_extract(pc.name, '.* ([A-Z].*)')) as label
FROM `${omopDataset}.icd_criteria` pc
WHERE pc.name not like '%EXPIRED%'
  and pc.is_root=false
