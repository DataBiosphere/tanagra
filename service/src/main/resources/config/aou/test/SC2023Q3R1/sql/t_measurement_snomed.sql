SELECT *
FROM `all-of-us-ehr-dev.SC2023Q3R1.concept` c
WHERE domain_id = 'Measurement'
  AND vocabulary_id = 'SNOMED'
  AND standard_concept = 'S'