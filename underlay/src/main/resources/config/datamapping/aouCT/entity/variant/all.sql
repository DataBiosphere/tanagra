SELECT ROW_NUMBER() OVER (ORDER BY vid) AS row_num,
       vid,
       gene_symbol,
       consequence,
       aa_change,
       clinvar_classification,
       gvs_all_ac,
       gvs_all_an,
       gvs_all_af
FROM `${omopDataset}.prep_vat`
