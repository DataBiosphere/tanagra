SELECT
    o.observation_id,
    o.person_id,
    (CASE WHEN o.observation_concept_id IS NULL THEN 0 ELSE o.observation_concept_id END) AS observation_concept_id,
    oc.concept_name AS observation_concept_name,
    o.observation_datetime,
    o.value_as_number,
    o.value_as_concept_id,
    evc.concept_name AS value_as_concept_name,
    o.unit_concept_id,
    uc.concept_name AS unit_concept_name,
    o.observation_source_value,
    o.observation_source_concept_id,
    CAST(FLOOR(TIMESTAMP_DIFF(o.observation_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
    o.visit_occurrence_id,
    vo.visit_concept_id,
    vc.concept_name AS visit_concept_name,
    true as pregnant_at_enrollment
FROM `${omopDataset}.observation` AS o
JOIN `${omopDataset}.person` AS p ON p.person_id = o.person_id
LEFT JOIN `${omopDataset}.concept` AS oc ON oc.concept_id = o.observation_concept_id
LEFT JOIN `${omopDataset}.concept` AS evc ON evc.concept_id = o.value_as_concept_id
LEFT JOIN `${omopDataset}.concept` AS uc ON uc.concept_id = o.unit_concept_id
LEFT JOIN `${omopDataset}.visit_occurrence` AS vo ON vo.visit_occurrence_id = o.visit_occurrence_id
LEFT JOIN `${omopDataset}.concept` AS vc ON vc.concept_id = vo.visit_concept_id
WHERE o.observation_source_concept_id = 903120
  AND o.value_as_concept_id = 45877994
