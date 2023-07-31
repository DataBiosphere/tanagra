SELECT p.date AS date, p.id AS id, p.person_id AS person_id, p.procedure AS procedure, p.source_criteria_id AS source_criteria_id, p.source_value AS source_value, p.t_display_procedure AS t_display_procedure, p.visit_occurrence_id AS visit_occurrence_id FROM `broad-tanagra-dev.aousynthetic_index_072623`.procedure_occurrence AS p WHERE p.id IN (SELECT i.id_procedure_occurrence FROM `broad-tanagra-dev.aousynthetic_index_072623`.idpairs_procedure_occurrence_person AS i WHERE i.id_person IN (SELECT p.id FROM `broad-tanagra-dev.aousynthetic_index_072623`.person AS p WHERE p.id IN (SELECT i.id_person FROM `broad-tanagra-dev.aousynthetic_index_072623`.idpairs_procedure_occurrence_person AS i WHERE i.id_procedure_occurrence IN (SELECT p.id FROM `broad-tanagra-dev.aousynthetic_index_072623`.procedure_occurrence AS p WHERE p.id IN (SELECT i.id_procedure_occurrence FROM `broad-tanagra-dev.aousynthetic_index_072623`.idpairs_procedure_occurrence_procedure AS i WHERE i.id_procedure IN (SELECT p.id FROM `broad-tanagra-dev.aousynthetic_index_072623`.procedure AS p WHERE p.id = 4324693)))))) LIMIT 30
