SELECT DISTINCT c.concept_id, c.concept_name, c.concept_code,
                CASE
                    WHEN c.standard_concept IS NULL THEN 'Source'
                    WHEN c.standard_concept = 'S' THEN 'Standard'
                    ELSE 'Unknown' END standard_concept,
                v.vocabulary_name

FROM `all-of-us-ehr-dev.SR2023Q3R1.device_exposure` AS de

JOIN `all-of-us-ehr-dev.SR2023Q3R1.concept` AS c
ON de.device_concept_id = c.concept_id

JOIN `all-of-us-ehr-dev.SR2023Q3R1.vocabulary` AS v
ON c.vocabulary_id = v.vocabulary_id

WHERE c.domain_id = 'Device'
  AND c.standard_concept = 'S'
  AND de.device_concept_id != 0
