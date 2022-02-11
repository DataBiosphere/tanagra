-- BuildTextSearchInformation search-strings input query for visits.
SELECT
  c.concept_id AS node, c.concept_name AS text
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c

UNION ALL

SELECT
  c.concept_id AS node, CAST(c.concept_id AS STRING) AS text
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c

UNION ALL

SELECT
  c.concept_id AS node, c.concept_code AS text
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c

UNION ALL

SELECT
  cs.concept_id AS node, cs.concept_synonym_name AS text
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept_synonym` cs
;
