/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.spi;

import org.hibernate.property.access.internal.PropertyAccessStrategyBasicImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyEmbeddedImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyFieldImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyMapImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyMixedImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyNoopImpl;

/**
 * Describes the built-in externally-nameable {@link PropertyAccessStrategy} implementations.
 *
 * @author Steve Ebersole
 */
public enum BuiltInPropertyAccessStrategies {
	BASIC( "property", PropertyAccessStrategyBasicImpl.INSTANCE ),
	FIELD( "field", PropertyAccessStrategyFieldImpl.INSTANCE ),
	MIXED( "mixed", PropertyAccessStrategyMixedImpl.INSTANCE ),
	MAP( "map", PropertyAccessStrategyMapImpl.INSTANCE ),
	EMBEDDED( "embedded", PropertyAccessStrategyEmbeddedImpl.INSTANCE ),
	NOOP( "noop", PropertyAccessStrategyNoopImpl.INSTANCE );

	private final String externalName;
	private final PropertyAccessStrategy strategy;

	BuiltInPropertyAccessStrategies(String externalName, PropertyAccessStrategy strategy) {
		this.externalName = externalName;
		this.strategy = strategy;
	}

	public String getExternalName() {
		return externalName;
	}

	public PropertyAccessStrategy getStrategy() {
		return strategy;
	}

	public static BuiltInPropertyAccessStrategies interpret(String name) {
		for ( BuiltInPropertyAccessStrategies strategy : values() ) {
			if ( strategy.externalName.equals( name ) ) {
				return strategy;
			}
		}
		return null;
	}
}
