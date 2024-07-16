SELECT
    cast(parse_numeric(dc.criteria_meta_seq) as INT64) as id,
    cast(parse_numeric(dc.criteria_meta_seq) as INT64) as concept_id,
    dc.name,
    'Source' as is_standard,
    cast(d.drug_rxcui_code as STRING) as concept_code,
    concat(cast(parse_numeric(dc.criteria_meta_seq) as STRING),' ',dc.name) as label,
FROM `${omopDataset}.med_criteria` dc
JOIN (SELECT distinct drug_name, drug_rxcui_code from `${omopDataset}.meds`) d
    ON lower(dc.name) = lower(d.drug_name)
