SELECT EXTRACT(YEAR FROM CURRENT_DATE()) - p.age AS age, p.ethnicity AS ethnicity, p.gender AS gender, p.id AS id, p.race AS race, p.t_display_ethnicity AS t_display_ethnicity, p.t_display_gender AS t_display_gender, p.t_display_race AS t_display_race, p.year_of_birth AS year_of_birth FROM `broad-tanagra-dev.cmssynpuf_index_082523`.person AS p WHERE p.id IN (SELECT d.id_person FROM `broad-tanagra-dev.cmssynpuf_index_082523`.device_person_occurrence_device_occurrence_person_idpairs AS d WHERE d.id_device_occurrence IN (SELECT d.id FROM `broad-tanagra-dev.cmssynpuf_index_082523`.device_occurrence AS d WHERE d.id IN (SELECT d.id_device_occurrence FROM `broad-tanagra-dev.cmssynpuf_index_082523`.device_person_occurrence_device_occurrence_device_idpairs AS d WHERE d.id_device IN (SELECT d.id FROM `broad-tanagra-dev.cmssynpuf_index_082523`.device AS d WHERE d.id = 4038664)))) LIMIT 30
