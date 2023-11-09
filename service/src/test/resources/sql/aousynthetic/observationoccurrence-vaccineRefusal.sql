SELECT o.date AS date, o.id AS id, o.observation AS observation, o.person_id AS person_id, o.source_criteria_id AS source_criteria_id, o.source_value AS source_value, o.t_display_observation AS t_display_observation, o.t_display_unit AS t_display_unit, o.t_display_value AS t_display_value, o.unit AS unit, o.value AS value, o.value_as_string AS value_as_string, o.visit_occurrence_id AS visit_occurrence_id FROM `broad-tanagra-dev.aousynthetic_index_082523`.observation_occurrence AS o WHERE o.person_id IN (SELECT p.id FROM `broad-tanagra-dev.aousynthetic_index_082523`.person AS p WHERE p.id IN (SELECT o.person_id FROM `broad-tanagra-dev.aousynthetic_index_082523`.observation_occurrence AS o WHERE o.observation IN (SELECT o.id FROM `broad-tanagra-dev.aousynthetic_index_082523`.observation AS o WHERE o.id = 43531662))) LIMIT 30