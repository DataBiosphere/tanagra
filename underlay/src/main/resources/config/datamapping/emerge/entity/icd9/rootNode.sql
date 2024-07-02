SELECT cast(cast(pc.criteria_meta_seq as numeric) as int64) as id
FROM `${omopDataset}.icd_criteria` pc
WHERE cast(cast(pc.parent_seq as numeric) as int64) = 0
