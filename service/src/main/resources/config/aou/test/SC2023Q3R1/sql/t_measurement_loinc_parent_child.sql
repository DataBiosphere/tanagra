SELECT
  cr.concept_id_1 AS parent,
  cr.concept_id_2 AS child,
FROM `all-of-us-ehr-dev.SC2023Q3R1.concept_relationship` cr
JOIN `all-of-us-ehr-dev.SC2023Q3R1.concept` c1  ON c1.concept_id = cr.concept_id_1
JOIN `all-of-us-ehr-dev.SC2023Q3R1.concept` c2  ON c2.concept_id = cr.concept_id_2
WHERE
  cr.relationship_id = 'Subsumes'
  AND c1.vocabulary_id = c2.vocabulary_id
  AND c1.vocabulary_id = 'LOINC'
  AND c1.concept_class_id IN ('LOINC Hierarchy', 'LOINC Component', 'Lab Test')
  AND c2.concept_class_id IN ('LOINC Hierarchy', 'LOINC Component', 'Lab Test')