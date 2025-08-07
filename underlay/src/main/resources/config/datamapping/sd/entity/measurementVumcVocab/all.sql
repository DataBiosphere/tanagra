SELECT
    c.concept_id,
    c.concept_name,
    c.vocabulary_id,
    c.concept_code,
    -- standard_concept is empty in concept table
    'Source' AS standard_concept
FROM (SELECT DISTINCT measurement_concept_id FROM `${omopDataset}.measurement`
      WHERE measurement_concept_id IS NOT NULL
        AND measurement_concept_id != 0) m
JOIN `${omopDataset}.concept` c ON m.measurement_concept_id = c.concept_id
WHERE c.domain_id = 'Measurement'
  AND c.vocabulary_id = 'VUMC Meas Value'
