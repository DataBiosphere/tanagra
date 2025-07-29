SELECT po.procedure_occurrence_id, pc.id AS cpt4_id
FROM `${omopDataset}.procedure_occurrence` AS po
RIGHT JOIN `${staticTablesDataset}.prep_cpt` AS pc
    ON pc.concept_id = po.procedure_source_concept_id
WHERE pc.type = 'CPT4'
  AND po.procedure_source_concept_id IS NOT null
  AND po.procedure_source_concept_id != 0
