SELECT
    c.concept_id AS id, c.concept_name AS name, c.vocabulary_id, c.standard_concept, c.concept_code
FROM `victr-tanagra-test.sd_static.concept` c
WHERE c.domain_id = 'Drug'
  AND (
        (c.vocabulary_id = 'ATC' AND c.standard_concept = 'C')
        OR (c.vocabulary_id IN ('RxNorm', 'RxNorm Extension') AND c.standard_concept = 'S')
    )