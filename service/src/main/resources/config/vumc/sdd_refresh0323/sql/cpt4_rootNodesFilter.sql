SELECT pc.id

FROM `sd-vumc-tanagra-test.aou_static_prep.prep_cpt` pc

WHERE pc.type = 'CPT4' AND pc.parent_id = 0
