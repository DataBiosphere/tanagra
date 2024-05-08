SELECT DISTINCT(concept_id) concept_id,
    concept_name,
    vocabulary_id,
    concept_code,
    standard_concept
FROM (
    SELECT
        concept_id,
        concept_name,
        vocabulary_id,
        concept_code,
        'Standard' AS standard_concept
    FROM `${omopDataset}.concept`
    WHERE
        (vocabulary_id = 'ATC' AND standard_concept = 'C')
        OR (vocabulary_id = 'RxNorm' AND standard_concept = 'S')

    UNION ALL

    SELECT
        c2.concept_id,
        c2.concept_name,
        c2.vocabulary_id,
        c2.concept_code,
        'Standard' AS standard_concept
    FROM `${omopDataset}.concept_ancestor` ca
    JOIN `${omopDataset}.concept` c1
        ON c1.concept_id = ca.ancestor_concept_id
        AND c1.vocabulary_id = 'RxNorm'
        AND c1.concept_class_id = 'Ingredient'
    JOIN `${omopDataset}.concept` c2
        ON c2.concept_id = ca.descendant_concept_id

    UNION ALL

    SELECT
        1 AS concept_id,
        'Unmapped' AS concept_name,
        'RxNorm' AS vocabulary_id,
        'Unmapped' AS concept_code,
        'Standard' AS standard_concept
)
