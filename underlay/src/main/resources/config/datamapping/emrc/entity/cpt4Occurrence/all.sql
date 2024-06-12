SELECT
    c.IND_CODE_SEQ as cpt4_occurrence_id,
    c.IND_SEQ as person_id,
    cc.criteria_meta_seq as cpt4_concept_id,
    regexp_replace(cc.name,concat(regexp_replace(cc.label, r'CPT Codes_Include_', ''),' '),'') as cpt4_concept_name,
    regexp_replace(cc.label, r'CPT Codes_Include_', '') as standard_code,
    CAST(FLOOR(cast(c.AGE_AT_EVENT as NUMERIC)) AS INT64) AS age_at_occurrence
FROM `${omopDataset}.cpt_codes` c
JOIN `${omopDataset}.cpt_criteria` cc
  ON c.code = regexp_replace(cc.label, r'CPT Codes_Include_', '')
      and cc.is_leaf = true
