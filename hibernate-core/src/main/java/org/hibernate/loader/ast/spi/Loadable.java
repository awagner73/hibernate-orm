/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.spi;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.sql.ast.tree.from.RootTableGroupProducer;

/**
 * Common details for things that can be loaded by a {@linkplain Loader loader} - generally
 * {@linkplain org.hibernate.metamodel.mapping.EntityMappingType entities} and
 * {@linkplain org.hibernate.metamodel.mapping.PluralAttributeMapping plural attributes} (collections).
 *
 * @see Loader
 * @see org.hibernate.metamodel.mapping.EntityMappingType
 * @see org.hibernate.metamodel.mapping.PluralAttributeMapping
 *
 * @author Steve Ebersole
 */
public interface Loadable extends ModelPart, RootTableGroupProducer {
	/**
	 * The name for this loadable, for use as the root when generating
	 * {@linkplain org.hibernate.spi.NavigablePath relative paths}
	 */
	String getRootPathName();

	/**
	 * Whether any of the "influencers" affect this loadable.
	 */
	boolean isAffectedByEnabledFilters(LoadQueryInfluencers influencers);

	/**
	 * Whether the {@linkplain LoadQueryInfluencers#getEffectiveEntityGraph() effective entity-graph}
	 * applies to this loadable
	 */
	boolean isAffectedByEntityGraph(LoadQueryInfluencers influencers);

	/**
	 * Whether any of the {@linkplain LoadQueryInfluencers#getEnabledFetchProfileNames()}
	 * apply to this loadable
	 */
	boolean isAffectedByEnabledFetchProfiles(LoadQueryInfluencers influencers);
}
