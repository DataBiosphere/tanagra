SELECT
    DISTINCT concept_id,
    concept_name,
    vocabulary_id,
    concept_code,
    (CASE WHEN standard_concept IS NULL THEN 'Source' WHEN standard_concept = 'S' THEN 'Standard' ELSE 'Unknown' END) AS standard_concept

FROM `${omopDataset}.visit_occurrence` AS vo

JOIN `${omopDataset}.concept` c
    ON vo.visit_concept_id = c.concept_id

WHERE c.domain_id = 'Visit'
  AND c.standard_concept = 'S'
  AND vo.visit_concept_id > 0
