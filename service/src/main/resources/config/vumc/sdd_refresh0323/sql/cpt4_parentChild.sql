SELECT
    pc.id AS child,
    pc.parent_id AS parent

FROM `victr-tanagra-test.aou_static_prep.prep_cpt` pc

WHERE pc.type = 'CPT4'
AND pc.parent_id != 0
