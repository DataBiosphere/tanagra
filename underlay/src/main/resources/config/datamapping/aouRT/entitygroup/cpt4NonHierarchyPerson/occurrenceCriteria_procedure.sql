SELECT po.procedure_occurrence_id, c.concept_id AS cpt4_id
FROM `${omopDataset}.procedure_occurrence` AS po
    JOIN `${omopDataset}.concept` AS c ON c.concept_code = po.procedure_source_value
WHERE c.vocabulary_id = 'CPT4'
  AND c.concept_code NOT IN (
    SELECT pc.code FROM `${staticTablesDataset}.prep_cpt` pc WHERE pc.type='CPT4'
  )

