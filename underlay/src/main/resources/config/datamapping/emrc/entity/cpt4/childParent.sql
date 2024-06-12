SELECT
    pc.criteria_meta_seq AS child,
    pc.parent_seq AS parent
FROM `${omopDataset}.cpt_criteria` pc
WHERE pc.parent_seq != 0
