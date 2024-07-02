SELECT
    cast(parse_numeric(lc.criteria_meta_seq) as INT64) as id,
    cast(parse_numeric(lc.criteria_meta_seq) as INT64) as concept_id,
    lc.name,
    'Source' as is_standard,
    cast(parse_numeric(lc.criteria_meta_seq) as STRING) as concept_code,
    concat(cast(parse_numeric(lc.criteria_meta_seq) as STRING),' ',lc.name) as label,
FROM `${omopDataset}.lab_criteria` lc
