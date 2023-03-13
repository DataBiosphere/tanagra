SELECT d.device AS device, d.end_date AS end_date, d.id AS id, d.person_id AS person_id, d.source_criteria_id AS source_criteria_id, d.source_value AS source_value, d.start_date AS start_date, d.t_display_device AS t_display_device, d.visit_occurrence_id AS visit_occurrence_id FROM `broad-tanagra-dev.aousynthetic_index_031323`.device_occurrence AS d WHERE d.id IN (SELECT i.id_device_occurrence FROM `broad-tanagra-dev.aousynthetic_index_031323`.idpairs_device_occurrence_person AS i WHERE i.id_person IN (SELECT p.id FROM `broad-tanagra-dev.aousynthetic_index_031323`.person AS p WHERE p.id IN (SELECT i.id_person FROM `broad-tanagra-dev.aousynthetic_index_031323`.idpairs_device_occurrence_person AS i WHERE i.id_device_occurrence IN (SELECT d.id FROM `broad-tanagra-dev.aousynthetic_index_031323`.device_occurrence AS d WHERE d.id IN (SELECT i.id_device_occurrence FROM `broad-tanagra-dev.aousynthetic_index_031323`.idpairs_device_occurrence_device AS i WHERE i.id_device IN (SELECT d.id FROM `broad-tanagra-dev.aousynthetic_index_031323`.device AS d WHERE d.id = 4038664)))))) LIMIT 30
