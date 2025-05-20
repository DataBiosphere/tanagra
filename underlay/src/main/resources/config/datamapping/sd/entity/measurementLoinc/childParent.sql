SELECT
    cr.concept_id_1 AS parent,
    cr.concept_id_2 AS child,
FROM `${omopDataset}.concept_relationship` cr
         JOIN `${omopDataset}.concept` parnt  ON cr.concept_id_1 = parnt.concept_id
         JOIN `${omopDataset}.concept` chld  ON cr.concept_id_2 = chld.concept_id
WHERE
    cr.relationship_id = 'Subsumes'
  AND parnt.vocabulary_id = chld.vocabulary_id
  AND parnt.vocabulary_id = 'LOINC'
  AND parnt.concept_class_id IN ('LOINC Hierarchy', 'LOINC Component', 'Lab Test')
  AND chld.concept_class_id IN ('LOINC Hierarchy', 'LOINC Component', 'Lab Test')
