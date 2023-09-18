SELECT textsearch.id, textsearch.text FROM (

    SELECT
      c.concept_id AS id, c.concept_name AS text
    FROM  `all-of-us-ehr-dev.SC2023Q3R1.concept` c

    UNION ALL

    SELECT
      c.concept_id AS id, CAST(c.concept_id AS STRING) AS text
    FROM  `all-of-us-ehr-dev.SC2023Q3R1.concept` c

    UNION ALL

    SELECT
      c.concept_id AS id, c.concept_code AS text
    FROM  `all-of-us-ehr-dev.SC2023Q3R1.concept` c
) AS textsearch

JOIN `all-of-us-ehr-dev.SC2023Q3R1.concept` c
ON c.concept_id = textsearch.id

WHERE c.domain_id = 'Device'
AND c.standard_concept = 'S'
