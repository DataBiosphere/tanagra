SELECT
    pc.criteria_meta_seq AS id,
    pc.criteria_meta_seq AS concept_id,
    REGEXP_EXTRACT(label, r'^PHEWAS_[0-9.]+-(.*)') AS name,
    pc.type,
    'Source' AS is_standard,
    REGEXP_EXTRACT(label, r'^PHEWAS_([0-9.]+)-') AS concept_code,
    REGEXP_EXTRACT(label, r'^PHEWAS_(.*)') AS label
FROM `${omopDataset}.phewas_criteria` pc
