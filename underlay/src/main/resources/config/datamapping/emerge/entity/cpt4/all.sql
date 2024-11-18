SELECT
    pc.criteria_meta_seq as id,
    pc.criteria_meta_seq as concept_id,
    regexp_replace(pc.name,concat(regexp_replace(pc.label, r'CPT Codes_Include_', ''),' '),'') as name,
    pc.type,
    'Source' AS is_standard,
    regexp_replace(pc.label, r'CPT Codes_Include_', '') as concept_code,
    name as label
FROM `${omopDataset}.cpt_criteria` pc
