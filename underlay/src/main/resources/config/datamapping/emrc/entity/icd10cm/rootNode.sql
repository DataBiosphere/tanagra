SELECT cast(cast(pc.criteria_meta_seq as numeric) as int64) as id
FROM `${omopDataset}.icd10_criteria` pc
WHERE cast(cast(pc.parent_seq as numeric) as int64) = 0
      and  not regexp_contains(pc.name, 'PCS')
