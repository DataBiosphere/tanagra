SELECT pc.criteria_meta_seq as id
FROM `${omopDataset}.cpt_criteria` pc
WHERE pc.parent_seq = 0
