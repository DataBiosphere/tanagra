SELECT pc.criteria_meta_seq as id
FROM `${omopDataset}.icd10_criteria` pc
WHERE pc.parent_seq = (
    select criteria_meta_seq from `${omopDataset}.icd10_criteria`
    where starts_with(label, 'ICD10CM')
    )

