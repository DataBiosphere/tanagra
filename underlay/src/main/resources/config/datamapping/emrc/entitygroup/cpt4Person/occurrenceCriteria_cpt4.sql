SELECT c.IND_CODE_SEQ as cpt4_occurrence_id,
       cc.criteria_meta_seq AS cpt4_id
FROM `${omopDataset}.cpt_codes` AS c
JOIN `${omopDataset}.cpt_criteria` AS cc
ON c.code = regexp_replace(cc.label, r'CPT Codes_Include_', '')
   and cc.is_leaf = true
