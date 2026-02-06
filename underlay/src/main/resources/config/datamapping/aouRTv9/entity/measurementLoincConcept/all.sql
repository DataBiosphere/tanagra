SELECT
    concept_id,
    concept_name,
    vocabulary_id,
    concept_code,
    (CASE WHEN standard_concept IS NULL THEN 'Standard' WHEN standard_concept IN ('S', 'C') THEN 'Standard' ELSE 'Unknown' END) AS standard_concept
FROM `${omopDataset}.concept`
WHERE
    vocabulary_id = 'LOINC'
    AND concept_class_id IN ('LOINC Hierarchy', 'LOINC Component', 'Lab Test')
