package com.gitplex.commons.hibernate.jackson;

import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.gitplex.commons.hibernate.AbstractEntity;

@SuppressWarnings("serial")
public class HibernateAnnotationIntrospector extends AnnotationIntrospector {

	@Override
	public Version version() {
		return Version.unknownVersion();
	}

    @Override
    public boolean hasIgnoreMarker(AnnotatedMember m) {
        return m.hasAnnotation(Transient.class) || m.hasAnnotation(OneToMany.class);
    }

	@SuppressWarnings("unchecked")
	@Override
	public Object findSerializer(Annotated am) {
		if (am.hasAnnotation(ManyToOne.class)) {
			return new ManyToOneSerializer((Class<AbstractEntity>) am.getRawType());
		} else {
			return super.findDeserializer(am);
		}
	}

	@Override
	public Object findDeserializer(Annotated am) {
		if (am.hasAnnotation(ManyToOne.class)) {
			return new ManyToOneDeserializer(am.getRawType());
		} else {
			return super.findDeserializer(am);
		}
	}

}
