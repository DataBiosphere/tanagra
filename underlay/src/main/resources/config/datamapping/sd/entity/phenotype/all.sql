SELECT
    criteria_meta_seq AS id,
    type,
    REGEXP_EXTRACT(label, r'^PHEWAS_([0-9.]+)-') AS concept_code,
    REGEXP_EXTRACT(label, r'^PHEWAS_[0-9.]+-(.*)') AS name,
    REGEXP_EXTRACT(label, r'^PHEWAS_(.*)') AS label,
    PARSE_NUMERIC(REGEXP_EXTRACT(label, r'^PHEWAS_([0-9.]+)-')) AS numeric_code
FROM `${omopDataset}.phewas_criteria`
