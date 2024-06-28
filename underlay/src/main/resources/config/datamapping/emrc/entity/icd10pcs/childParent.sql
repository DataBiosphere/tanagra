SELECT
    pc.criteria_meta_seq AS child,
    pc.parent_seq AS parent
FROM  `${omopDataset}.icd10_criteria` pc
WHERE pc.parent_seq >= (
    select criteria_meta_seq from `${omopDataset}.icd10_criteria`
    where starts_with(label, 'ICD10PCS')
    )
