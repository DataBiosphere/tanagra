SELECT c.condition AS condition, c.end_date AS end_date, c.id AS id, c.person_id AS person_id, c.source_criteria_id AS source_criteria_id, c.source_value AS source_value, c.start_date AS start_date, c.stop_reason AS stop_reason, c.t_display_condition AS t_display_condition, c.visit_occurrence_id AS visit_occurrence_id FROM `broad-tanagra-dev.aousynthetic_index_011523`.condition_occurrence AS c WHERE c.id IN (SELECT i.id_condition_occurrence FROM `broad-tanagra-dev.aousynthetic_index_011523`.idpairs_condition_occurrence_person AS i WHERE i.id_person IN (SELECT p.id FROM `broad-tanagra-dev.aousynthetic_index_011523`.person AS p WHERE p.id IN (SELECT i.id_person FROM `broad-tanagra-dev.aousynthetic_index_011523`.idpairs_condition_occurrence_person AS i WHERE i.id_condition_occurrence IN (SELECT c.id FROM `broad-tanagra-dev.aousynthetic_index_011523`.condition_occurrence AS c WHERE c.id IN (SELECT i.id_condition_occurrence FROM `broad-tanagra-dev.aousynthetic_index_011523`.idpairs_condition_occurrence_condition AS i WHERE i.id_condition IN (SELECT c.id FROM `broad-tanagra-dev.aousynthetic_index_011523`.condition AS c WHERE c.id = 201826)))))) LIMIT 30
