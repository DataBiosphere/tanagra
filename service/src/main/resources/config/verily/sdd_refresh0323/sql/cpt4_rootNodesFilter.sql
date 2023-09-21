SELECT pc.id

FROM `verily-tanagra-dev.aou_static_prep_uscentral1.prep_cpt` pc

WHERE pc.type = 'CPT4' AND pc.parent_id = 0
