package com.manydesigns.portofino.model.io.dsl;

import com.manydesigns.portofino.model.*;
import com.manydesigns.portofino.model.language.ModelBaseVisitor;
import com.manydesigns.portofino.model.language.ModelParser;
import org.antlr.v4.runtime.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class EntityModelVisitor extends ModelBaseVisitor<ModelObject> {

    protected final Model model;
    protected Domain parentDomain;
    protected Entity entity;
    protected Annotated annotated;

    public EntityModelVisitor(Model model) {
        this(model, null);
    }

    public EntityModelVisitor(Model model, Domain parentDomain) {
        this.model = model;
        this.parentDomain = parentDomain;
    }

    @Override
    public Domain visitStandaloneDomain(ModelParser.StandaloneDomainContext ctx) {
        return visitDomain(ctx.domain());
    }

    @Override
    public Domain visitDomain(ModelParser.DomainContext ctx) {
        Domain previous = parentDomain;
        String name = ctx.name.getText();
        Domain domain;
        if(parentDomain != null) {
            domain = parentDomain.ensureSubdomain(name);
        } else {
            domain = model.ensureDomain(name);
        }
        parentDomain = domain;
        annotated = domain;
        visitChildren(ctx);
        parentDomain = previous;
        annotated = previous;
        return domain;
    }

    @Override
    public ModelObject visitStandaloneEntity(ModelParser.StandaloneEntityContext ctx) {
        return super.visitStandaloneEntity(ctx);
    }

    @Override
    public Entity visitEntity(ModelParser.EntityContext ctx) {
        String entityName = ctx.name.getText();
        if(parentDomain == null) {
            throw new IllegalStateException("Entity definition without a domain: " + entityName);
        }
        Entity previous = entity;
        entity = parentDomain.ensureEntity(entityName);
        annotated = entity;
        visitChildren(ctx);
        ctx.idProperties.forEach(idp -> {
            String propertyName = idp.name.getText();
            Optional<Property> property = entity.getProperties().stream().filter(
                    p -> p.getName().equals(propertyName)
            ).findFirst();
            if(property.isPresent()) {
                entity.getId().add(property.get());
            } else {
                throw new IllegalStateException("Id property not found: " + propertyName);
            }
        });
        entity = previous;
        annotated = previous;
        return entity;
    }

    @Override
    public Annotation visitAnnotation(ModelParser.AnnotationContext ctx) {
        String annotationType = ctx.name.getText();
        if(annotated == null) {
            throw new IllegalStateException("Annotation without an element: " + annotationType);
        }
        Annotation annotation = annotated.ensureAnnotation(resolveAnnotationType(annotationType));
        ModelParser.AnnotationParamsContext params = ctx.annotationParams();
        if(params != null) {
            visitAnnotationParams(annotation, params);
        }
        return annotation;
    }

    public void visitAnnotationParams(Annotation annotation, ModelParser.AnnotationParamsContext params) {
        if (params.IDENTIFIER().isEmpty()) {
            annotation.getProperties().add(new AnnotationProperty("value", getText(params.literal(0))));
        } else {
            for (int i = 0; i < params.IDENTIFIER().size(); i++) {
                AnnotationProperty prop =
                        new AnnotationProperty(params.IDENTIFIER(i).getText(), getText(params.literal(i)));
                annotation.getProperties().add(prop);
            }
        }
    }

    @Override
    public Property visitProperty(ModelParser.PropertyContext ctx) {
        String propertyName = ctx.name.getText();
        Annotated previous = annotated;
        if(entity == null) {
            throw new IllegalStateException("Property without an entity: " + propertyName);
        }
        Type type;
        if(ctx.type() != null) {
            String typeName = ctx.type().name.getText();
            type = entity.getDomain().findType(typeName);
            if(type == null) {
                throw new RuntimeException("Unknown type: " + typeName); //TODO
            }
        } else {
            type = entity.getDomain().getDefaultType();
            if(type == null) {
                throw new RuntimeException("Domain " + entity.getDomain() + " does not have a default type"); //TODO
            }
        }

        Property property = entity.findProperty(propertyName);
        if(property == null) {
            property = new Property();
            property.setName(propertyName);
            entity.addProperty(property);
        }
        property.setType(type);
        annotated = property;
        visitChildren(ctx);
        annotated = previous;
        return property;
    }

    protected String decodeType(String typeName) {
        return typeName;
    }

    protected String resolveAnnotationType(String annotationType) {
        return annotationType;
    }

    private String getText(ModelParser.LiteralContext value) {
        String text = value.getText();
        if(value.STRING() != null) {
            text = text.substring(1, text.length() - 1);
        }
        return text;
    }
}
