SELECT textsearch.id, textsearch.text FROM (

    SELECT c.concept_id AS id, c.concept_name AS text
    FROM `all-of-us-ehr-dev.SC2023Q3R1.concept` c

    UNION ALL

    SELECT c.concept_id AS id, CAST(c.concept_id AS STRING) AS text
    FROM `all-of-us-ehr-dev.SC2023Q3R1.concept` c

    UNION ALL

    SELECT c.concept_id AS id, c.concept_code AS text
    FROM `all-of-us-ehr-dev.SC2023Q3R1.concept` c

    UNION ALL

    SELECT cs.concept_id AS id, cs.concept_synonym_name AS text
    FROM `all-of-us-ehr-dev.SC2023Q3R1.concept` c1
    JOIN `all-of-us-ehr-dev.SC2023Q3R1.concept_synonym` cs ON c1.concept_id = cs.concept_id

) AS textsearch
JOIN `all-of-us-ehr-dev.SC2023Q3R1.concept` c ON c.concept_id = textsearch.id
WHERE c.domain_id = 'Measurement'
  AND c.vocabulary_id = 'SNOMED'
  AND c.standard_concept = 'S'