SELECT
    c.concept_id,
    c.concept_name,
    c.vocabulary_id,
    c.concept_code,
    'Standard' AS standard_concept
FROM (SELECT DISTINCT condition_concept_id FROM `${omopDataset}.condition_occurrence`
      WHERE condition_concept_id IS NOT NULL
        AND condition_concept_id != 0) co
JOIN `${omopDataset}.concept` c on co.condition_concept_id = c.concept_id
    AND c.domain_id = 'Condition'
    AND c.vocabulary_id != 'SNOMED'
    AND c.standard_concept = 'S'
