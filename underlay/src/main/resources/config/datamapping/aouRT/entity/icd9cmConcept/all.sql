SELECT
    cc.concept_id,
    cc.concept_name,
    cc.vocabulary_id,
    (CASE WHEN cc.standard_concept IS NULL THEN 'Source' WHEN cc.standard_concept = 'S' THEN 'Standard' ELSE 'Unknown' END) AS standard_concept,
    cc.concept_code,
    CASE WHEN cc.concept_code IS NULL THEN cc.concept_name ELSE CONCAT(cc.concept_code, ' ', cc.concept_name) END AS label,
    co.condition_start_datetime,
    co.visit_occurrence_id,
FROM `${omopDataset}.concept` as cc
JOIN `${omopDataset}.condition_occurrence` AS co ON co.condition_concept_id = cc.concept_id
WHERE
  cc.vocabulary_id = 'ICD9CM'
  AND DATE_DIFF(CAST(cc.valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0

UNION ALL

SELECT
    pc.concept_id,
    pc.concept_name,
    pc.vocabulary_id,
    (CASE WHEN pc.standard_concept IS NULL THEN 'Source' WHEN pc.standard_concept = 'S' THEN 'Standard' ELSE 'Unknown' END) AS standard_concept,
    pc.concept_code,
    CASE WHEN pc.concept_code IS NULL THEN pc.concept_name ELSE CONCAT(pc.concept_code, ' ', pc.concept_name) END AS label,
    co.condition_start_datetime,
    co.visit_occurrence_id,
FROM `${staticTablesDataset}.prep_concept` as pc
JOIN `${omopDataset}.condition_occurrence` AS co ON co.condition_concept_id = pc.concept_id
WHERE
  pc.vocabulary_id = 'ICD9CM'
  AND DATE_DIFF(CAST(pc.valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0
