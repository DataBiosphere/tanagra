
    SELECT
        p.year_of_birth      
    FROM
        ${ENT_person} AS p      
    WHERE
        p.person_id = @val
