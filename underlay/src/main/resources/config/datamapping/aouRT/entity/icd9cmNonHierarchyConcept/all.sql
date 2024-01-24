SELECT
    concept_id,
    concept_name,
    vocabulary_id,
    (CASE WHEN standard_concept IS NULL THEN 'Source' WHEN standard_concept = 'S' THEN 'Standard' ELSE 'Unknown' END) AS standard_concept,
    concept_code,
    CASE WHEN concept_code IS NULL THEN concept_name ELSE CONCAT(concept_code, ' ', concept_name) END AS label
FROM `${omopDataset}.concept` a
WHERE
  vocabulary_id = 'ICD9CM'
  AND concept_id IN (select distinct condition_source_concept_id from `${omopDataset}.condition_occurrence`)
  AND concept_id NOT IN(
    SELECT concept_id FROM `${omopDataset}.concept`
    WHERE vocabulary_id = 'ICD9CM'
      AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0
    UNION ALL
    SELECT concept_id FROM `${staticTablesDataset}.prep_concept`
    WHERE vocabulary_id = 'ICD9CM'
      AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0
    )

