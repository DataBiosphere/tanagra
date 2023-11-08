
    SELECT
        t.T_DISP_measurement AS T_DISP_measurement,
        t.T_DISP_unit AS T_DISP_unit,
        t.T_DISP_value_enum AS T_DISP_value_enum,
        t.T_DISP_visit_type AS T_DISP_visit_type,
        t.age_at_occurrence AS age_at_occurrence,
        t.date AS date,
        t.id AS id,
        t.measurement AS measurement,
        t.person_id AS person_id,
        t.source_criteria_id AS source_criteria_id,
        t.source_value AS source_value,
        t.unit AS unit,
        t.value_enum AS value_enum,
        t.value_numeric AS value_numeric,
        t.visit_occurrence_id AS visit_occurrence_id,
        t.visit_type AS visit_type 
    FROM
        `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_measurementOccurrence AS t 
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
                        `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_measurementOccurrence AS t 
                    WHERE
                        t.measurement IN (
                            SELECT
                                t.id 
                            FROM
                                `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_measurementLoinc AS t 
                            WHERE
                                t.id = 3009542
                        )
                    )
            ) LIMIT 30
