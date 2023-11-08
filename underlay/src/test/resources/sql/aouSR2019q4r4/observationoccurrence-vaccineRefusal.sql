
    SELECT
        t.T_DISP_observation AS T_DISP_observation,
        t.T_DISP_unit AS T_DISP_unit,
        t.T_DISP_value AS T_DISP_value,
        t.T_DISP_visit_type AS T_DISP_visit_type,
        t.age_at_occurrence AS age_at_occurrence,
        t.date AS date,
        t.id AS id,
        t.observation AS observation,
        t.person_id AS person_id,
        t.source_criteria_id AS source_criteria_id,
        t.source_value AS source_value,
        t.unit AS unit,
        t.value AS value,
        t.value_as_string AS value_as_string,
        t.visit_occurrence_id AS visit_occurrence_id,
        t.visit_type AS visit_type 
    FROM
        `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_observationOccurrence AS t 
    WHERE
        t.person_id IN (
            SELECT
                t.id 
            FROM
                `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_person AS t 
            WHERE
                t.id IN (
                    SELECT
                        t.person_id 
                    FROM
                        `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_observationOccurrence AS t 
                    WHERE
                        t.observation IN (
                            SELECT
                                t.id 
                            FROM
                                `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_observation AS t 
                            WHERE
                                t.id = 43531662
                        )
                    )
            ) LIMIT 30
