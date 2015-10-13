/*
 * Copyright 2011-2015 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.b2international.snowowl.snomed.importer.rf2.validation;

import static com.google.common.collect.Sets.newHashSet;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.common.SnomedRf2Headers;
import com.b2international.snowowl.snomed.importer.net4j.DefectType;
import com.b2international.snowowl.snomed.importer.net4j.ImportConfiguration;
import com.b2international.snowowl.snomed.importer.release.ReleaseFileSet.ReleaseComponentType;
import com.b2international.snowowl.snomed.importer.rf2.model.ComponentImportType;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Represents a release file validator that validates the description release file.
 */
public class SnomedDescriptionValidator extends AbstractSnomedValidator {
	
	private final Map<String, List<String>> descriptionIdsWithEffectivetimeStatus = Maps.newHashMap();
	private final Map<String, List<String>> fullySpecifiedNames = Maps.newHashMap();
	private final Map<String, String> fullySpecifiedNameNotUnique = Maps.newHashMap();
	private final Set<String> descriptionIdNotUnique = Sets.newHashSet();
	private final Set<String> descriptionConceptNotExist = Sets.newHashSet();
	private final Set<String> typeConceptNotExist = Sets.newHashSet();
	private final Set<String> caseSignificanceConceptNotExist = Sets.newHashSet();

	public SnomedDescriptionValidator(final ImportConfiguration configuration, final SnomedValidationContext context) throws IOException {
		super(configuration, configuration.toURL(configuration.getDescriptionsFile()), ComponentImportType.DESCRIPTION, context, SnomedRf2Headers.DESCRIPTION_HEADER);
	}

	@Override
	protected void doValidate(final List<String> row, final int lineNumber) {
		final String componentId = row.get(0);
		final boolean active = "1".equals(row.get(2));
		
		registerComponent(ComponentCategory.DESCRIPTION, componentId, active);
		
		final String concept = row.get(4);
		final String type = row.get(6);
		final String caseSignificance = row.get(8);
		
		validateComponentExists(concept, concept, ReleaseComponentType.CONCEPT, descriptionConceptNotExist, lineNumber);
		validateComponentExists(type, concept, ReleaseComponentType.CONCEPT, typeConceptNotExist, lineNumber);
		validateComponentExists(caseSignificance, concept, ReleaseComponentType.CONCEPT, caseSignificanceConceptNotExist, lineNumber);
		
		validateComponentUnique(row, descriptionIdsWithEffectivetimeStatus, descriptionIdNotUnique, lineNumber);
		validateFullySpecifiedName(row, lineNumber);
	}
	
	@Override
	protected void doValidate(IProgressMonitor monitor) {
		super.doValidate(monitor);
		addDefect(DefectType.NOT_UNIQUE_DESCRIPTION_ID, descriptionIdNotUnique);
		addDefect(DefectType.NOT_UNIQUE_FULLY_SPECIFIED_NAME, newHashSet(fullySpecifiedNameNotUnique.values()));
		addDefect(DefectType.DESCRIPTION_CONCEPT_NOT_EXIST, descriptionConceptNotExist);
		addDefect(DefectType.DESCRIPTION_TYPE_NOT_EXIST, typeConceptNotExist);
		addDefect(DefectType.DESCRIPTION_CASE_SIGNIFICANCE_NOT_EXIST, caseSignificanceConceptNotExist);
	}
	
	private void validateFullySpecifiedName(final List<String> row, final int lineNumber) {
		final String concept = row.get(4);
		final String type = row.get(6);
		if (Concepts.FULLY_SPECIFIED_NAME.equals(type)) {
			final String term = row.get(7);
			if (fullySpecifiedNames.containsKey(term)) {
				final String active = row.get(2);
				if (fullySpecifiedNames.get(term).get(0).equals(concept)) {
					fullySpecifiedNames.get(term).set(1, active);
				} else if (!fullySpecifiedNames.get(term).get(1).equals("0")) {
					if (active.equals("0")) {
						fullySpecifiedNameNotUnique.remove(term);
					} else {
						if (isComponentActive(concept)) {
							fullySpecifiedNameNotUnique.put(term, MessageFormat.format("Line number {0} in the ''{1}'' file with term {2}", 
									lineNumber, releaseFileName, term));
						}
					}
				}
			} else {
				if (isComponentActive(concept)) {
					fullySpecifiedNames.put(term, createConceptIdStatusList(row));
				}
			}
		}
	}

}
