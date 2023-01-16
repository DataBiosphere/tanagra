/*
 platform_id column has type NUMERIC. This causes index column to be FLOAT.
 Currently indexing doesn't handle floats properly, so change column to INTEGER.
*/
SELECT CAST(platform_id as INT64) AS platform_id, assay_name as assay_name
FROM `victr-tanagra-test.sd_static.platform`
