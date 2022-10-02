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

    UNION ALL

    SELECT
      cs.concept_id AS id, cs.concept_synonym_name AS text
    FROM  `victr-tanagra-test.sd_static.concept_synonym` cs
) AS textsearch

JOIN `victr-tanagra-test.sd_static.concept` c
ON c.concept_id = textsearch.id

WHERE c.domain_id = 'Condition'
