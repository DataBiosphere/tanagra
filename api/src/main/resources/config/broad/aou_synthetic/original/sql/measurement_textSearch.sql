SELECT textsearch.id, textsearch.text FROM (

    SELECT
      c.concept_id AS id, c.concept_name AS text
    FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c

    UNION ALL

    SELECT
      c.concept_id AS id, CAST(c.concept_id AS STRING) AS text
    FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c

    UNION ALL

    SELECT
      c.concept_id AS id, c.concept_code AS text
    FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c

    UNION ALL

    SELECT
      cs.concept_id AS id, cs.concept_synonym_name AS text
    FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept_synonym` cs
) AS textsearch

JOIN `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c
ON c.concept_id = textsearch.id

WHERE ((c.domain_id = 'Measurement' AND c.vocabulary_id = 'SNOMED')
        OR (c.vocabulary_id = 'LOINC' AND c.concept_class_id IN ('LOINC Hierarchy', 'LOINC Component', 'Lab Test')))
AND c.valid_end_date > DATE('2022-01-01')