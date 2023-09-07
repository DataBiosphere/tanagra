SELECT textsearch.id, textsearch.text FROM (

    SELECT
      c.concept_id AS id, c.concept_name AS text
    FROM  `all-of-us-ehr-dev.SR2023Q3R1.concept` c

    UNION ALL

    SELECT
      c.concept_id AS id, CAST(c.concept_id AS STRING) AS text
    FROM  `all-of-us-ehr-dev.SR2023Q3R1.concept` c

    UNION ALL

    SELECT
      c.concept_id AS id, c.concept_code AS text
    FROM  `all-of-us-ehr-dev.SR2023Q3R1.concept` c
) AS textsearch

JOIN `all-of-us-ehr-dev.SR2023Q3R1.concept` c
ON c.concept_id = textsearch.id

WHERE c.domain_id = 'Drug'
AND ((c.vocabulary_id IN ('RxNorm', 'RxNorm Extension')
        AND c.concept_class_id = 'Ingredient'
        AND c.standard_concept = 'S')
    OR (c.vocabulary_id = 'RxNorm'
        AND c.concept_class_id = 'Precise Ingredient')
    OR (c.vocabulary_id = 'ATC'
        AND c.concept_class_id IN ('ATC 1st', 'ATC 2nd', 'ATC 3rd', 'ATC 4th', 'ATC 5th')
        AND c.standard_concept = 'C'))
