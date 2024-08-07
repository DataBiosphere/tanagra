SELECT
    cast(cast(pc.criteria_meta_seq as numeric) as int64) AS child,
    cast(cast(pc.parent_seq as numeric) as int64) AS parent
FROM `${omopDataset}.icd_criteria` pc
WHERE cast(cast(parent_seq as numeric) as int64) != 0
