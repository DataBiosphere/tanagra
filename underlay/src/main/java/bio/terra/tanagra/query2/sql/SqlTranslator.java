package bio.terra.tanagra.query2.sql;

import bio.terra.tanagra.api.field.valuedisplay.AttributeField;
import bio.terra.tanagra.api.field.valuedisplay.EntityIdCountField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyIsMemberField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyIsRootField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyNumChildrenField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyPathField;
import bio.terra.tanagra.api.field.valuedisplay.RelatedEntityIdCountField;
import bio.terra.tanagra.api.field.valuedisplay.ValueDisplayField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.BooleanNotFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.HierarchyHasParentFilter;
import bio.terra.tanagra.api.filter.HierarchyIsMemberFilter;
import bio.terra.tanagra.api.filter.HierarchyIsRootFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.api.filter.TextSearchFilter;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.query2.sql.filtertranslator.BooleanAndOrFilterTranslator;
import bio.terra.tanagra.query2.sql.filtertranslator.BooleanNotFilterTranslator;

public interface SqlTranslator {
    SqlFieldTranslator translator(AttributeField attributeField);
    SqlFieldTranslator translator(EntityIdCountField entityIdCountField);
    SqlFieldTranslator translator(HierarchyIsMemberField hierarchyIsMemberField);
    SqlFieldTranslator translator(HierarchyIsRootField hierarchyIsRootField);
    SqlFieldTranslator translator(HierarchyNumChildrenField hierarchyNumChildrenField);
    SqlFieldTranslator translator(HierarchyPathField hierarchyPathField);
    SqlFieldTranslator translator(RelatedEntityIdCountField relatedEntityIdCountField);

    default SqlFieldTranslator translator(ValueDisplayField valueDisplayField) {
        if (valueDisplayField instanceof AttributeField) {
            return translator((AttributeField) valueDisplayField);
        } else if (valueDisplayField instanceof EntityIdCountField) {
            return translator((EntityIdCountField) valueDisplayField);
        } else if (valueDisplayField instanceof HierarchyIsMemberField) {
            return translator((HierarchyIsMemberField) valueDisplayField);
        } else if (valueDisplayField instanceof HierarchyIsRootField) {
            return translator((HierarchyIsRootField) valueDisplayField);
        } else if (valueDisplayField instanceof HierarchyNumChildrenField) {
            return translator((HierarchyNumChildrenField) valueDisplayField);
        } else if (valueDisplayField instanceof HierarchyPathField) {
            return translator((HierarchyPathField) valueDisplayField);
        } else if (valueDisplayField instanceof RelatedEntityIdCountField) {
            return translator((RelatedEntityIdCountField) valueDisplayField);
        } else {
            throw new InvalidQueryException("No SQL translator defined for field");
        }
    }

    SqlFilterTranslator translator(AttributeFilter attributeFilter);
    default SqlFilterTranslator translator(BooleanAndOrFilter booleanAndOrFilter) {
        return new BooleanAndOrFilterTranslator(this, booleanAndOrFilter);
    }
    default SqlFilterTranslator translator(BooleanNotFilter booleanNotFilter) {
        return new BooleanNotFilterTranslator(this, booleanNotFilter);
    }
    SqlFilterTranslator translator(HierarchyHasAncestorFilter hierarchyHasAncestorFilter);
    SqlFilterTranslator translator(HierarchyHasParentFilter hierarchyHasParentFilter);
    SqlFilterTranslator translator(HierarchyIsMemberFilter hierarchyIsMemberFilter);
    SqlFilterTranslator translator(HierarchyIsRootFilter hierarchyIsRootFilter);
    SqlFilterTranslator translator(RelationshipFilter relationshipFilter);
    SqlFilterTranslator translator(TextSearchFilter textSearchFilter);

    default SqlFilterTranslator translator(EntityFilter entityFilter) {
        if (entityFilter instanceof AttributeFilter) {
            return translator((AttributeFilter) entityFilter);
        } else if (entityFilter instanceof BooleanAndOrFilter) {
            return translator((BooleanAndOrFilter) entityFilter);
        } else if (entityFilter instanceof BooleanNotFilter) {
            return translator((BooleanNotFilter) entityFilter);
        } else if (entityFilter instanceof HierarchyHasAncestorFilter) {
            return translator((HierarchyHasAncestorFilter) entityFilter);
        } else if (entityFilter instanceof HierarchyHasParentFilter) {
            return translator((HierarchyHasParentFilter) entityFilter);
        } else if (entityFilter instanceof HierarchyIsMemberFilter) {
            return translator((HierarchyIsMemberFilter) entityFilter);
        } else if (entityFilter instanceof HierarchyIsRootFilter) {
            return translator((HierarchyIsRootFilter) entityFilter);
        } else if (entityFilter instanceof RelationshipFilter) {
            return translator((RelationshipFilter) entityFilter);
        } else if (entityFilter instanceof TextSearchFilter) {
            return translator((TextSearchFilter) entityFilter);
        } else {
            throw new InvalidQueryException("No SQL translator defined for filter");
        }
    }
}
