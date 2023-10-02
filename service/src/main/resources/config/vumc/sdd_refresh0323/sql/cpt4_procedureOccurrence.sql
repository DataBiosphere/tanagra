SELECT po.procedure_occurrence_id, pc.id AS cpt4_id
FROM `sd-vumc-tanagra-test.sd_20230331.procedure_occurrence` AS po

JOIN `sd-vumc-tanagra-test.aou_static_prep.prep_cpt` AS pc
ON pc.concept_id = po.procedure_source_concept_id

WHERE pc.type = 'CPT4'