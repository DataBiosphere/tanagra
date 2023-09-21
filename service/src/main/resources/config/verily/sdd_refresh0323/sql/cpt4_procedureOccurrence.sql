SELECT po.procedure_occurrence_id, pc.id AS cpt4_id
FROM `sd-vumc-tanagra-test.sd_20230331.procedure_occurrence` AS po

JOIN `verily-tanagra-dev.aou_static_prep_uscentral1.prep_cpt` AS pc
ON pc.concept_id = po.procedure_source_concept_id

WHERE pc.type = 'CPT4'