-- BuildTextSearchInformation search-strings input query for ingredients.
SELECT
  c.concept_id AS node, c.concept_name AS text
FROM  `victr-tanagra-test.sd_static.concept` c

UNION ALL

SELECT
  c.concept_id AS node, CAST(c.concept_id AS STRING) AS text
FROM  `victr-tanagra-test.sd_static.concept` c

UNION ALL

SELECT
  c.concept_id AS node, c.concept_code AS text
FROM  `victr-tanagra-test.sd_static.concept` c
