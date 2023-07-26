SELECT textsearch.id, textsearch.text FROM (

    SELECT
      c.concept_id AS id, c.concept_name AS text
    FROM  `victr-tanagra-test.sd_20230328.concept` c

    UNION ALL

    SELECT
      c.concept_id AS id, CAST(c.concept_id AS STRING) AS text
    FROM  `victr-tanagra-test.sd_20230328.concept` c

    UNION ALL

    SELECT
      c.concept_id AS id, c.concept_code AS text
    FROM  `victr-tanagra-test.sd_20230328.concept` c
) AS textsearch

JOIN `victr-tanagra-test.sd_20230328.concept` c
ON c.concept_id = textsearch.id

WHERE ((c.domain_id = 'Measurement' AND c.vocabulary_id = 'SNOMED')
        OR (c.vocabulary_id = 'LOINC' AND c.concept_class_id IN ('LOINC Hierarchy', 'LOINC Component', 'Lab Test')))