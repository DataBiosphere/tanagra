SELECT procedure_occurrence_alias2.procedure_occurrence_id AS procedure_occurrence_id, procedure_occurrence_alias2.person_id AS person_id, procedure_occurrence_alias2.procedure_concept_id AS procedure_concept_id, procedure_occurrence_alias2.procedure_date AS procedure_date, procedure_occurrence_alias2.visit_occurrence_id AS visit_occurrence_id, procedure_occurrence_alias2.procedure_source_value AS procedure_source_value, procedure_occurrence_alias2.procedure_source_concept_id AS procedure_source_concept_id FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.procedure_occurrence AS procedure_occurrence_alias2 WHERE procedure_occurrence_alias2.person_id IN (SELECT person_alias.person_id FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.person AS person_alias WHERE person_alias.person_id IN (SELECT procedure_occurrence_alias1.person_id FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.procedure_occurrence AS procedure_occurrence_alias1 WHERE procedure_occurrence_alias1.procedure_concept_id IN (SELECT procedure_alias.concept_id FROM (SELECT * FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept WHERE domain_id = 'Procedure' AND valid_end_date > '2022-01-01') AS procedure_alias WHERE procedure_alias.concept_id = 4324693)))
