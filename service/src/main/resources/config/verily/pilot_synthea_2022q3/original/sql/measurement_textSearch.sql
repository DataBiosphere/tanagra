SELECT textsearch.id, textsearch.text FROM (

    SELECT
      c.concept_id AS id, c.concept_name AS text
    FROM  `verily-tanagra-dev.pilot_synthea_2022q3.concept` c

    UNION ALL

    SELECT
      c.concept_id AS id, CAST(c.concept_id AS STRING) AS text
    FROM  `verily-tanagra-dev.pilot_synthea_2022q3.concept` c

    UNION ALL

    SELECT
      c.concept_id AS id, c.concept_code AS text
    FROM  `verily-tanagra-dev.pilot_synthea_2022q3.concept` c
) AS textsearch

JOIN `verily-tanagra-dev.pilot_synthea_2022q3.concept` c
ON c.concept_id = textsearch.id

WHERE ((c.domain_id = 'Measurement' AND c.vocabulary_id = 'SNOMED')
        OR (c.vocabulary_id = 'LOINC' AND c.concept_class_id IN ('LOINC Hierarchy', 'LOINC Component', 'Lab Test')))