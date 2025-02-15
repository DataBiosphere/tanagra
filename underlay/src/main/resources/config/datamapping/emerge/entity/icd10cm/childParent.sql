/*
Relationships between levels 1 and 2, which includes the AoU/VUMC-defined organizational codes.
*/
SELECT
  cr.concept_id_1 AS parent,
  cr.concept_id_2 AS child,
FROM `${staticTablesDataset}.prep_concept_relationship` cr
JOIN `${staticTablesDataset}.prep_concept` c1  ON c1.concept_id = cr.concept_id_1
JOIN `${staticTablesDataset}.prep_concept` c2  ON c2.concept_id = cr.concept_id_2
WHERE
  cr.relationship_id = 'Subsumes'
  AND c1.vocabulary_id = c2.vocabulary_id
  AND c2.vocabulary_id = 'ICD10CM'

UNION ALL

/*
Relationships between levels 2 and 3, which is the link between the standard ICD-10-CM codes and the AoU/VUMC-defined organizational codes.
*/
SELECT
    p.concept_id AS parent,
    c.concept_id AS child
FROM `${staticTablesDataset}.prep_concept` p
LEFT JOIN `${omopDataset}.concept` AS c
    ON c.vocabulary_id = p.vocabulary_id
    AND UPPER(c.concept_code) >= UPPER(REGEXP_EXTRACT(p.concept_code, r'^([a-zA-Z0-9]+)-'))
    AND UPPER(c.concept_code) <= UPPER(REGEXP_EXTRACT(p.concept_code, r'-([a-zA-Z0-9]+)$'))
    AND NOT CONTAINS_SUBSTR(c.concept_code, '.')
WHERE p.vocabulary_id='ICD10CM' AND c.concept_id IS NOT NULL

UNION ALL

/*
Relationships between levels 3+, which includes the standard ICD-10-CM defined codes.
*/
SELECT
  cr.concept_id_1 AS parent,
  cr.concept_id_2 AS child,
FROM `${omopDataset}.concept_relationship` cr
JOIN `${omopDataset}.concept` c1  ON c1.concept_id = cr.concept_id_1
JOIN `${omopDataset}.concept` c2  ON c2.concept_id = cr.concept_id_2
WHERE
  cr.relationship_id = 'Subsumes'
  AND c1.vocabulary_id = c2.vocabulary_id
  AND c2.vocabulary_id = 'ICD10CM'
  AND LENGTH(REGEXP_REPLACE(c1.concept_code, r'\.', '')) = LENGTH(REGEXP_REPLACE(c2.concept_code, r'\.', '')) - 1
