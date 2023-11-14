
    SELECT
        e.T_DISP_observation AS T_DISP_observation,
        e.T_DISP_unit AS T_DISP_unit,
        e.T_DISP_value AS T_DISP_value,
        e.age_at_occurrence AS age_at_occurrence,
        e.date AS date,
        e.id AS id,
        e.observation AS observation,
        e.person_id AS person_id,
        e.source_criteria_id AS source_criteria_id,
        e.source_value AS source_value,
        e.unit AS unit,
        e.value AS value,
        e.value_as_string AS value_as_string,
        e.visit_occurrence_id AS visit_occurrence_id 
    FROM
        `verily-tanagra-dev.cmssynpuf_index_110623`.ENT_observationOccurrence AS e 
    WHERE
        e.person_id IN (
            SELECT
                e.id 
            FROM
                `verily-tanagra-dev.cmssynpuf_index_110623`.ENT_person AS e 
            WHERE
                e.id IN (
                    SELECT
                        e.person_id 
                    FROM
                        `verily-tanagra-dev.cmssynpuf_index_110623`.ENT_observationOccurrence AS e 
                    WHERE
                        e.observation IN (
                            SELECT
                                e.id 
                            FROM
                                `verily-tanagra-dev.cmssynpuf_index_110623`.ENT_observation AS e 
                            WHERE
                                e.id = 43531662
                        )
                    )
            ) LIMIT 30
