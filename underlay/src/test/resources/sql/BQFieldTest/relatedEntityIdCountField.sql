
    SELECT
        T_RCNT_conditionPerson_NOHIER,
        T_RCNT_conditionPerson_default      
    FROM
        ${ENT_condition}      
    ORDER BY
        T_RCNT_conditionPerson_NOHIER DESC,
        T_RCNT_conditionPerson_default ASC
