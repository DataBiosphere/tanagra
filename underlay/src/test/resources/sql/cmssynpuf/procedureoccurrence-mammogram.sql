
    SELECT
        e.T_DISP_procedure AS T_DISP_procedure,
        e.age_at_occurrence AS age_at_occurrence,
        e.date AS date,
        e.id AS id,
        e.person_id AS person_id,
        e.procedure AS procedure,
        e.source_criteria_id AS source_criteria_id,
        e.source_value AS source_value,
        e.visit_occurrence_id AS visit_occurrence_id 
    FROM
        `verily-tanagra-dev.cmssynpuf_index_110623`.ENT_procedureOccurrence AS e 
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
                        `verily-tanagra-dev.cmssynpuf_index_110623`.ENT_procedureOccurrence AS e 
                    WHERE
                        e.procedure IN (
                            SELECT
                                e.id 
                            FROM
                                `verily-tanagra-dev.cmssynpuf_index_110623`.ENT_procedure AS e 
                            WHERE
                                e.id = 4324693
                        )
                    )
            ) LIMIT 30
