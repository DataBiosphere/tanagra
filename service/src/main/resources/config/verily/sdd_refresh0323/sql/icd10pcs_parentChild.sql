/*
Relationships between levels 1 and 2, which is the link between the standard ICD-10-PCS codes and the AoU/VUMC-defined organizational codes.
*/
SELECT
    2500000022 AS parent,
    c.concept_id AS child
FROM `sd-vumc-tanagra-test.sd_20230331.concept` AS c
WHERE
    c.vocabulary_id = 'ICD10PCS'
    AND c.domain_id = 'Procedure'
    AND DATE_DIFF(CAST(c.valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0
    AND NOT EXISTS (
      SELECT cr.concept_id_1
      FROM `sd-vumc-tanagra-test.sd_20230331.concept_relationship` cr
      WHERE cr.concept_id_2 = c.concept_id
          AND cr.relationship_id = 'Subsumes'
    )

UNION ALL

/*
Relationships between levels 2+, which includes the standard ICD-10-PCS defined codes.
*/
SELECT
  cr.concept_id_1 AS parent,
  cr.concept_id_2 AS child,
FROM `sd-vumc-tanagra-test.sd_20230331.concept_relationship` cr
JOIN `sd-vumc-tanagra-test.sd_20230331.concept` c1  ON c1.concept_id = cr.concept_id_1
JOIN `sd-vumc-tanagra-test.sd_20230331.concept` c2  ON c2.concept_id = cr.concept_id_2
WHERE
  cr.relationship_id = 'Subsumes'
  AND c1.vocabulary_id = c2.vocabulary_id
  AND c2.vocabulary_id = 'ICD10PCS'
  AND LENGTH(REGEXP_REPLACE(c1.concept_code, r'\.', '')) = LENGTH(REGEXP_REPLACE(c2.concept_code, r'\.', '')) - 1
