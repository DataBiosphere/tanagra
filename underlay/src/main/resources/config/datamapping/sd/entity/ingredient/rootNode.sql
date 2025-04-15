SELECT
    concept_id
FROM `${omopDataset}.concept`
WHERE
    vocabulary_id = 'ATC'
    AND concept_class_id = 'ATC 1st'
    AND standard_concept = 'C'

UNION ALL

/* Unmapped */
SELECT 1
