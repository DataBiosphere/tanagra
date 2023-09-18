SELECT d.age_at_occurrence AS age_at_occurrence, d.device AS device, d.end_date AS end_date, d.id AS id, d.person_id AS person_id, d.source_criteria_id AS source_criteria_id, d.source_value AS source_value, d.start_date AS start_date, d.t_display_device AS t_display_device FROM `broad-tanagra-dev.cmssynpuf_index_082523`.device_occurrence AS d WHERE d.person_id IN (SELECT p.id FROM `broad-tanagra-dev.cmssynpuf_index_082523`.person AS p WHERE p.id IN (SELECT d.person_id FROM `broad-tanagra-dev.cmssynpuf_index_082523`.device_occurrence AS d WHERE d.device IN (SELECT d.id FROM `broad-tanagra-dev.cmssynpuf_index_082523`.device AS d WHERE d.id = 4038664))) LIMIT 30
