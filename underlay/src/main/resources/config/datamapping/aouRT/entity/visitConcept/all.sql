SELECT
    DISTINCT concept_id,
    concept_name,
    vocabulary_id,
    concept_code,
    'Standard'  AS standard_concept
FROM `${omopDataset}.visit_occurrence` AS vo
JOIN `${omopDataset}.concept` c ON vo.visit_concept_id = c.concept_id
    AND c.domain_id = 'Visit'
    AND c.standard_concept = 'S'
WHERE vo.visit_concept_id IS NOT null
    AND vo.visit_concept_id != 0
