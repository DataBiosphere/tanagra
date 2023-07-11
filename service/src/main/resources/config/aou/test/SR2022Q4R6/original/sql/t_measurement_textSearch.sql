SELECT textsearch.id, textsearch.text FROM (

    SELECT
      c.concept_id AS id, c.concept_name AS text
    FROM  `all-of-us-ehr-dev.SR2022Q4R6.concept` c

    UNION ALL

    SELECT
      c.concept_id AS id, CAST(c.concept_id AS STRING) AS text
    FROM  `all-of-us-ehr-dev.SR2022Q4R6.concept` c

    UNION ALL

    SELECT
      c.concept_id AS id, c.concept_code AS text
    FROM  `all-of-us-ehr-dev.SR2022Q4R6.concept` c
) AS textsearch

JOIN `all-of-us-ehr-dev.SR2022Q4R6.concept` c
ON c.concept_id = textsearch.id

WHERE ((c.domain_id = 'Measurement' AND c.vocabulary_id = 'SNOMED')
        OR (c.vocabulary_id = 'LOINC' AND c.concept_class_id IN ('LOINC Hierarchy', 'LOINC Component', 'Lab Test')))
