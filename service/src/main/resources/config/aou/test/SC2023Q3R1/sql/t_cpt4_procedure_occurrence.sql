SELECT po.procedure_occurrence_id, pc.id AS cpt4_id
FROM `all-of-us-ehr-dev.SC2023Q3R1.procedure_occurrence` AS po

JOIN `all-of-us-ehr-dev.SC2023Q3R1.prep_cpt` AS pc
ON pc.concept_id = po.procedure_source_concept_id

WHERE pc.type = 'CPT4'
