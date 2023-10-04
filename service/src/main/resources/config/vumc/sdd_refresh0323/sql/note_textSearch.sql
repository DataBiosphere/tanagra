SELECT textsearch.id, textsearch.text

FROM (
    SELECT
      c.concept_id AS id, c.concept_name AS text, c.vocabulary_id
    FROM  `sd-vumc-tanagra-test.sd_20230331.concept` c

    UNION ALL

    SELECT
      c.concept_id AS id, CAST(c.concept_id AS STRING) AS text, c.vocabulary_id
    FROM  `sd-vumc-tanagra-test.sd_20230331.concept` c

    UNION ALL

    SELECT
      c.concept_id AS id, c.concept_code AS text, c.vocabulary_id
    FROM  `sd-vumc-tanagra-test.sd_20230331.concept` c
) AS textsearch

WHERE vocabulary_id = 'Note Type' OR id = 0
