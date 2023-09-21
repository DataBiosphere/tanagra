SELECT textsearch.id, textsearch.text FROM (

    SELECT
      c.concept_id AS id, c.concept_name AS text
    FROM  `sd-vumc-tanagra-test.sd_20230331.concept` c

    UNION ALL

    SELECT
      c.concept_id AS id, CAST(c.concept_id AS STRING) AS text
    FROM  `sd-vumc-tanagra-test.sd_20230331.concept` c

    UNION ALL

    SELECT
      c.concept_id AS id, c.concept_code AS text
    FROM  `sd-vumc-tanagra-test.sd_20230331.concept` c
) AS textsearch

JOIN `sd-vumc-tanagra-test.sd_20230331.concept` c
ON c.concept_id = textsearch.id

WHERE c.domain_id = 'Procedure'
