SELECT textsearch.id, textsearch.text FROM (

    SELECT
      c.concept_id AS id, c.concept_name AS text
    FROM  `victr-tanagra-test.sd_static.concept` c

    UNION ALL

    SELECT
      c.concept_id AS id, CAST(c.concept_id AS STRING) AS text
    FROM  `victr-tanagra-test.sd_static.concept` c

    UNION ALL

    SELECT
      c.concept_id AS id, c.concept_code AS text
    FROM  `victr-tanagra-test.sd_static.concept` c
) AS textsearch

JOIN `victr-tanagra-test.sd_static.concept` c
ON c.concept_id = textsearch.id

WHERE c.domain_id = 'Drug'
AND c.concept_class_id = 'Brand Name'
AND c.vocabulary_id IN ('RxNorm', 'RxNorm Extension')