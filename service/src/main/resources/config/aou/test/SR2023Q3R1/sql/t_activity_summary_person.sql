SELECT fas.*
FROM
    `all-of-us-ehr-dev.SR2023Q3R1.activity_summary` fas,
    `all-of-us-ehr-dev.SR2023Q3R1.person` p
WHERE
       fas.person_id = p.person_id
