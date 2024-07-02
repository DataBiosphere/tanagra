SELECT c.ind_seq as person_id,
       cc.criteria_meta_seq AS cpt4_id
FROM `${omopDataset}.cpt_codes` AS c
JOIN `${omopDataset}.cpt_criteria` cc
    on c.code = regexp_replace(cc.label, r'CPT Codes_Include_', '')
