SELECT textsearch.id, textsearch.text FROM (

    SELECT c.concept_id AS id, c.concept_name AS text
    FROM `all-of-us-ehr-dev.SR2023Q3R1.concept` c
    WHERE domain_id = 'Measurement'
    AND vocabulary_id = 'SNOMED'
    AND standard_concept = 'S'

    UNION ALL

    SELECT c.concept_id AS id, CAST(c.concept_id AS STRING) AS text
    FROM `all-of-us-ehr-dev.SR2023Q3R1.concept` c
    WHERE domain_id = 'Measurement'
    AND vocabulary_id = 'SNOMED'
    AND standard_concept = 'S'

    UNION ALL

    SELECT c.concept_id AS id, c.concept_code AS text
    FROM `all-of-us-ehr-dev.SR2023Q3R1.concept` c
    WHERE domain_id = 'Measurement'
    AND vocabulary_id = 'SNOMED'
    AND standard_concept = 'S'

    UNION ALL

    SELECT cs.concept_id AS id, cs.concept_synonym_name AS text
    FROM `all-of-us-ehr-dev.SR2023Q3R1.concept` c1
    JOIN `all-of-us-ehr-dev.SR2023Q3R1.concept_synonym` cs ON c1.concept_id = cs.concept_id
    WHERE c1.domain_id = 'Measurement'
    AND c1.vocabulary_id = 'SNOMED'
    AND c1.standard_concept = 'S'

) AS textsearch
JOIN `all-of-us-ehr-dev.SR2023Q3R1.concept` c ON c.concept_id = textsearch.id