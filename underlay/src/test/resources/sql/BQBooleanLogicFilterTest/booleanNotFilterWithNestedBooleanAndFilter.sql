
    SELECT
        year_of_birth      
    FROM
        ${ENT_person}      
    WHERE
        NOT ((year_of_birth != @val0)          
        AND (gender = @val1))
