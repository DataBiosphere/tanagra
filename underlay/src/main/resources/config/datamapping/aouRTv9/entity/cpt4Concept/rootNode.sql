SELECT pc.id

FROM `${staticTablesDataset}.prep_cpt` pc

WHERE pc.type = 'CPT4' AND pc.parent_id = 0
