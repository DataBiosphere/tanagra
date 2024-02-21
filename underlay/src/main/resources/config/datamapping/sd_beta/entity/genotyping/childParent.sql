SELECT (100 + gc.parent_seq) AS parent, CAST(xp.platform_id AS INT64) AS child

FROM `${omopDataset}`.x_platform AS xp

JOIN `${omopDataset}`.genotype_criteria AS gc
  ON UPPER(gc.name) = UPPER(xp.assay_name) AND gc.type = 'DNA'
