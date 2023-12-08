
    SELECT
        c.T_RCNT_conditionPerson_NOHIER,
        c.T_RCNT_conditionPerson_default      
    FROM
        ${ENT_condition} AS c      
    ORDER BY
        T_RCNT_conditionPerson_NOHIER DESC,
        T_RCNT_conditionPerson_default ASC
