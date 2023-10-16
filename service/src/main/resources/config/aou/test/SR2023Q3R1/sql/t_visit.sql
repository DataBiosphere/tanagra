SELECT
  distinct c.concept_id, c.concept_name

FROM `all-of-us-ehr-dev.SR2023Q3R1.visit_occurrence` AS vo

JOIN `all-of-us-ehr-dev.SR2023Q3R1.concept` c
ON vo.visit_concept_id = c.concept_id

WHERE c.domain_id = 'Visit'
  AND c.standard_concept = 'S'
  AND vo.visit_concept_id > 0
