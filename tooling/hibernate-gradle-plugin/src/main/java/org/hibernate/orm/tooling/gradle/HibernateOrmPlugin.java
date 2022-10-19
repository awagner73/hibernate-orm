/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle;

import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JvmEcosystemPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.compile.JavaCompile;

import org.hibernate.orm.tooling.gradle.enhance.EnhancementHelper;
import org.hibernate.orm.tooling.gradle.metamodel.JpaMetamodelGenerationTask;

import static org.hibernate.orm.tooling.gradle.HibernateOrmSpec.HIBERNATE;
import static org.hibernate.orm.tooling.gradle.metamodel.JpaMetamodelGenerationTask.COMPILE_META_TASK_NAME;
import static org.hibernate.orm.tooling.gradle.metamodel.JpaMetamodelGenerationTask.GEN_TASK_NAME;

/**
 * Hibernate ORM Gradle plugin
 */
public class HibernateOrmPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		// for SourceSet support and other JVM goodies
		project.getPlugins().apply( JvmEcosystemPlugin.class );

		project.getLogger().debug( "Adding Hibernate extensions to the build [{}]", project.getPath() );
		final HibernateOrmSpec ormDsl = project.getExtensions().create( HibernateOrmSpec.DSL_NAME,  HibernateOrmSpec.class, project );

		prepareEnhancement( ormDsl, project );
		prepareModelGen( ormDsl, project );
		prepareHbmTransformation( ormDsl, project );


		//noinspection ConstantConditions
		project.getDependencies().add(
				"implementation",
				ormDsl.getUseSameVersion().map( (use) -> use
						? "org.hibernate.orm:hibernate-core:" + HibernateVersion.version
						: null
				)
		);
	}

	private void prepareEnhancement(HibernateOrmSpec ormDsl, Project project) {
		project.getGradle().getTaskGraph().whenReady( (graph) -> {
			if ( !ormDsl.isEnhancementEnabled() ) {
				return;
			}

			final SourceSet sourceSet = ormDsl.getSourceSet().get();
			final Set<String> languages = ormDsl.getLanguages().getOrNull();
			if ( languages == null ) {
				return;
			}

			for ( String language : languages ) {
				final String languageCompileTaskName = sourceSet.getCompileTaskName( language );
				final AbstractCompile languageCompileTask = (AbstractCompile) project.getTasks().findByName( languageCompileTaskName );
				if ( languageCompileTask == null ) {
					continue;
				}

				//noinspection Convert2Lambda
				languageCompileTask.doLast( new Action<>() {
					@Override
					public void execute(Task t) {
						final DirectoryProperty classesDirectory = languageCompileTask.getDestinationDirectory();
						final ClassLoader classLoader = Helper.toClassLoader( sourceSet, project );

						EnhancementHelper.enhance( classesDirectory, classLoader, ormDsl, project );
					}
				} );
			}
		} );
	}

	private void prepareModelGen(HibernateOrmSpec ormDsl, Project project) {
		final TaskProvider<JavaCompile> modelCompileTaskRef = project.getTasks().register( COMPILE_META_TASK_NAME, JavaCompile.class, (modelCompileTask) -> {
			modelCompileTask.onlyIf( (t) -> ormDsl.isMetamodelGenerationEnabled() );

			modelCompileTask.setGroup( HIBERNATE );
			modelCompileTask.setDescription( "Compiles the JPA static metamodel generated by `" + GEN_TASK_NAME + "`" );
		} );

		project.getTasks().register( GEN_TASK_NAME, JpaMetamodelGenerationTask.class, (genTask) -> {
			genTask.onlyIf( (t) -> ormDsl.isMetamodelGenerationEnabled() );

			if ( !ormDsl.isMetamodelGenerationEnabled() ) {
				return;
			}

			genTask.injectSourceSet( ormDsl.getSourceSet() );

			genTask.getGenerationOutputDirectory().set( ormDsl.getJpaMetamodel().getGenerationOutputDirectory() );

			genTask.getApplyGeneratedAnnotation().convention( ormDsl.getJpaMetamodel().getApplyGeneratedAnnotation() );
			genTask.getSuppressions().convention( ormDsl.getJpaMetamodel().getSuppressions() );

			final JavaCompile modelCompileTask = modelCompileTaskRef.get();

			final SourceSet sourceSet = ormDsl.getSourceSet().get();
			sourceSet.getAllSource().minus( sourceSet.getAllSource() ).forEach( (dir) -> {
				final String language = dir.getName();
				final String languageCompileTaskName = sourceSet.getCompileTaskName( language );
				final AbstractCompile languageCompileTask = (AbstractCompile) project.getTasks().getByName( languageCompileTaskName );
				genTask.dependsOn( languageCompileTask );

				modelCompileTask.setSourceCompatibility( languageCompileTask.getSourceCompatibility() );
				modelCompileTask.setTargetCompatibility( languageCompileTask.getTargetCompatibility() );

				modelCompileTask.finalizedBy( modelCompileTask );
			} );

			genTask.dependsOn( sourceSet.getProcessResourcesTaskName() );

			genTask.finalizedBy( modelCompileTask );
			modelCompileTask.dependsOn( genTask );
			modelCompileTask.source( project.files( ormDsl.getJpaMetamodel().getGenerationOutputDirectory() ) );
			modelCompileTask.getDestinationDirectory().set( ormDsl.getJpaMetamodel().getCompileOutputDirectory() );

			FileCollection metamodelCompileClasspath = project.getConfigurations().getByName( "runtimeClasspath" )
					.plus( sourceSet.getCompileClasspath() )
					.plus( sourceSet.getRuntimeClasspath() );
			if ( ormDsl.getJpaMetamodel().getApplyGeneratedAnnotation().getOrElse( true ) ) {
				final Dependency jakartaAnnotationsDep = project.getDependencies().create( "jakarta.annotation:jakarta.annotation-api:2.0.0" );
				metamodelCompileClasspath = metamodelCompileClasspath.plus(
						project.getConfigurations().detachedConfiguration( jakartaAnnotationsDep )
				);
			}

			modelCompileTask.setClasspath( metamodelCompileClasspath );
		} );
	}

	private void prepareHbmTransformation(HibernateOrmSpec ormDsl, Project project) {

	}
}
