SELECT (100 + gc.parent_seq) AS parent, CAST(xp.platform_id AS INT64) AS child

FROM `sd-vumc-tanagra-test.sd_20230331`.x_platform AS xp

JOIN `sd-vumc-tanagra-test.sd_20230331`.genotype_criteria AS gc
  ON gc.name = xp.assay_name AND gc.type = 'DNA'
