SELECT
    criteria_meta_seq AS id,
    REGEXP_EXTRACT(label, r'^PHEWAS_([0-9.]+)-') AS code,
    REGEXP_EXTRACT(label, r'^PHEWAS_[0-9.]+-(.*)') AS display_name,
    REGEXP_EXTRACT(label, r'^PHEWAS_(.*)') AS label,
    PARSE_NUMERIC(REGEXP_EXTRACT(label, r'^PHEWAS_([0-9.]+)-')) AS numeric_code
FROM `sd-vumc-tanagra-test.sd_20230331.phewas_criteria`