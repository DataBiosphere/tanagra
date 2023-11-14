
    SELECT
        e.T_DISP_device AS T_DISP_device,
        e.age_at_occurrence AS age_at_occurrence,
        e.device AS device,
        e.end_date AS end_date,
        e.id AS id,
        e.person_id AS person_id,
        e.source_criteria_id AS source_criteria_id,
        e.source_value AS source_value,
        e.start_date AS start_date,
        e.visit_occurrence_id AS visit_occurrence_id 
    FROM
        `verily-tanagra-dev.cmssynpuf_index_110623`.ENT_deviceOccurrence AS e 
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
                        `verily-tanagra-dev.cmssynpuf_index_110623`.ENT_deviceOccurrence AS e 
                    WHERE
                        e.device IN (
                            SELECT
                                e.id 
                            FROM
                                `verily-tanagra-dev.cmssynpuf_index_110623`.ENT_device AS e 
                            WHERE
                                e.id = 4038664
                        )
                    )
            ) LIMIT 30
