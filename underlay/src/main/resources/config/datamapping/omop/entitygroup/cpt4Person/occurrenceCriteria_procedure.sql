SELECT po.procedure_occurrence_id, pc.id AS cpt4_id
FROM `${omopDataset}.procedure_occurrence` AS po

JOIN `${staticTablesDataset}.prep_cpt` AS pc
    ON pc.concept_id = po.procedure_source_concept_id

WHERE pc.type = 'CPT4'