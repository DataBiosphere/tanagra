
    SELECT
        COUNT(DISTINCT person_id) AS T_CTDT,
        condition      
    FROM
        ${ENT_conditionOccurrence}      
    GROUP BY
        condition      
    ORDER BY
        T_CTDT DESC
