SELECT i.days_supply AS days_supply, i.end_date AS end_date, i.id AS id, i.ingredient AS ingredient, i.person_id AS person_id, i.refills AS refills, i.source_criteria_id AS source_criteria_id, i.source_value AS source_value, i.start_date AS start_date, i.stop_reason AS stop_reason, i.t_display_ingredient AS t_display_ingredient, i.visit_occurrence_id AS visit_occurrence_id FROM `broad-tanagra-dev.aousynthetic_index_082523`.ingredient_occurrence AS i WHERE i.person_id IN (SELECT p.id FROM `broad-tanagra-dev.aousynthetic_index_082523`.person AS p WHERE p.id IN (SELECT i.person_id FROM `broad-tanagra-dev.aousynthetic_index_082523`.ingredient_occurrence AS i WHERE i.ingredient IN (SELECT i.id FROM `broad-tanagra-dev.aousynthetic_index_082523`.ingredient AS i WHERE i.id = 1177480))) LIMIT 30