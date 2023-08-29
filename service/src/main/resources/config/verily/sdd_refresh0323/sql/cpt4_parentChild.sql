SELECT
    pc.id AS child,
    pc.parent_id AS parent

FROM `verily-tanagra-dev.aou_static_prep_useast1.prep_cpt` pc

WHERE pc.type = 'CPT4'
AND pc.parent_id != 0
