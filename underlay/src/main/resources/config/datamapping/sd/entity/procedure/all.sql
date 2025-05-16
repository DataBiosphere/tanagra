SELECT
    concept_id,
    concept_name,
    vocabulary_id,
    concept_code,
    'Standard' AS standard_concept
FROM `${omopDataset}.concept`
WHERE domain_id = 'Procedure'
  AND standard_concept = 'S'
  AND vocabulary_id = 'SNOMED'

-- UNION DISTINCT
-- -- 'HCPCS','LOINC' for non-hierarchy concepts
-- SELECT
--     concept_id,
--     concept_name,
--     vocabulary_id,
--     concept_code,
--     'Standard' AS standard_concept
-- FROM `${omopDataset}.concept` c
-- WHERE c.concept_id in (
--     SELECT DISTINCT procedure_concept_id from `${omopDataset}.procedure_occurrence`
--   )
--   AND domain_id = 'Procedure'
--   AND c.vocabulary_id IN ('HCPCS','LOINC')
--   AND c.standard_concept = 'S'
