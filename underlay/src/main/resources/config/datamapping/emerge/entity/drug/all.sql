SELECT
    cast(parse_numeric(dc.criteria_meta_seq) as INT64) as id,
    dc.name,
    cast(d.drug_rxcui_code as STRING) as concept_code,
    dc.name as label,
FROM `${omopDataset}.med_criteria` dc
JOIN (SELECT distinct drug_name, drug_rxcui_code from `${omopDataset}.meds`) d
    ON lower(dc.name) = lower(d.drug_name)
