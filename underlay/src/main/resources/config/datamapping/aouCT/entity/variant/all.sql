WITH sorted_transcripts AS (
    SELECT vid,
           consequence,
           aa_change,
           contig,
           position,
           ref_allele,
           alt_allele,
           dbsnp_rsid,
           transcript,
           dna_change_in_transcript,
           clinvar_classification,
           gvs_all_ac,
           gvs_all_an,
           gvs_all_af,
           ROW_NUMBER() OVER(
                PARTITION BY vid ORDER BY
                    CASE ARRAY_TO_STRING(consequence, ', ')
                        WHEN 'upstream_gene_variant'
                            THEN 4
                        WHEN 'downstream_gene_variant'
                            THEN 5
                        ELSE 1
                    END) AS row_number
    FROM `${omopDataset}.prep_vat`
    WHERE is_canonical_transcript OR transcript IS NULL
    ORDER BY vid, row_number),

    genes AS (
         SELECT vid, ARRAY_AGG(DISTINCT gene_symbol IGNORE NULLS ORDER BY gene_symbol) AS genes
         FROM `${omopDataset}.prep_vat`
         GROUP BY vid
    )

SELECT
    sorted_transcripts.vid,
    genes.genes as gene_symbol,
    sorted_transcripts.dbsnp_rsid,
    sorted_transcripts.consequence,
    sorted_transcripts.aa_change,
    sorted_transcripts.clinvar_classification,
    sorted_transcripts.gvs_all_ac,
    sorted_transcripts.gvs_all_an,
    sorted_transcripts.gvs_all_af,
    sorted_transcripts.contig,
    sorted_transcripts.position
FROM sorted_transcripts, genes
WHERE genes.vid = sorted_transcripts.vid
  AND (sorted_transcripts.row_number =1 or sorted_transcripts.transcript is NULL)
