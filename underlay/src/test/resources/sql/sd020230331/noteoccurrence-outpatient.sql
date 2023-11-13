
    SELECT
        e.T_DISP_note AS T_DISP_note,
        e.T_DISP_visit_type AS T_DISP_visit_type,
        e.age_at_occurrence AS age_at_occurrence,
        e.date AS date,
        e.id AS id,
        e.note AS note,
        e.note_text AS note_text,
        e.person_id AS person_id,
        e.source_value AS source_value,
        e.title AS title,
        e.visit_occurrence_id AS visit_occurrence_id,
        e.visit_type AS visit_type 
    FROM
        `verily-tanagra-dev.sd20230331_index_110623`.ENT_noteOccurrence AS e 
    WHERE
        e.person_id IN (
            SELECT
                e.id 
            FROM
                `verily-tanagra-dev.sd20230331_index_110623`.ENT_person AS e 
            WHERE
                e.id IN (
                    SELECT
                        e.person_id 
                    FROM
                        `verily-tanagra-dev.sd20230331_index_110623`.ENT_noteOccurrence AS e 
                    WHERE
                        e.note IN (
                            SELECT
                                e.id 
                            FROM
                                `verily-tanagra-dev.sd20230331_index_110623`.ENT_note AS e 
                            WHERE
                                e.id = 44814638
                        )
                    )
            ) LIMIT 30
