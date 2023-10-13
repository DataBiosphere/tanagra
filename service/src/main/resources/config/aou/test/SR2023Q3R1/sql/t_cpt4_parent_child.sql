SELECT
    pc.id AS child,
    pc.parent_id AS parent

FROM `all-of-us-ehr-dev.SR2023Q3R1.prep_cpt` pc

WHERE pc.type = 'CPT4'
AND pc.parent_id != 0
