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
package com.b2international.snowowl.snomed.importer.rf2.net4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.cdo.util.CommitException;

import com.b2international.commons.CompareUtils;
import com.b2international.commons.csv.CsvLexer.EOL;
import com.b2international.commons.csv.CsvParser;
import com.b2international.commons.csv.CsvSettings;
import com.b2international.commons.csv.RecordParserCallback;
import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.api.SnowowlServiceException;
import com.b2international.snowowl.core.api.browser.ITerminologyBrowser;
import com.b2international.snowowl.core.date.DateFormats;
import com.b2international.snowowl.core.date.EffectiveTimes;
import com.b2international.snowowl.core.domain.TransactionContext;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.datastore.cdo.ICDOConnection;
import com.b2international.snowowl.datastore.cdo.ICDOConnectionManager;
import com.b2international.snowowl.datastore.server.snomed.ImportOnlySnomedTransactionContext;
import com.b2international.snowowl.snomed.Component;
import com.b2international.snowowl.snomed.Concept;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.SnomedPackage;
import com.b2international.snowowl.snomed.common.SnomedTerminologyComponentConstants;
import com.b2international.snowowl.snomed.core.domain.CharacteristicType;
import com.b2international.snowowl.snomed.core.domain.ReservingIdStrategy;
import com.b2international.snowowl.snomed.datastore.SnomedEditingContext;
import com.b2international.snowowl.snomed.datastore.SnomedTerminologyBrowser;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptIndexEntry;
import com.b2international.snowowl.snomed.datastore.request.SnomedConceptCreateRequestBuilder;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.b2international.snowowl.snomed.importer.net4j.SnomedUnimportedRefSets;
import com.b2international.snowowl.snomed.refset.core.automap.CsvVariableFieldCountParser;
import com.b2international.snowowl.snomed.refset.core.automap.XlsParser;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;

/**
 * Import simple type reference sets into Snow Owl from a SNOMED&nbsp;CT RF1
 * subset file.
 * 
 * 
 */
public class SnomedSubsetImporter {

	private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SnomedSubsetImporter.class);
	
	private final boolean isUiImport;
	private final boolean hasHeader;
	private final boolean skipEmptyLines;
	private final int idColumnNumber;
	private final int firstConceptRowNumber;
	private final int sheetNumber;
	private final int refSetType;
	private final EOL lineFeedCharacter;
	private final File importFile;
	private char fieldSeparatorCharacter;
	private final String fileExtension;
	private final IBranchPath branchPath;
	private final String quoteCharacter;
	private final String effectiveTime;
	private final String subsetName;
	private final String namespace;
	private SnomedUnimportedRefSets unimportedRefSets;
	private final String userId;
	private final Set<String> importedConceptIds;

	public SnomedSubsetImporter(final String branchPath, String userId, boolean hasHeader, boolean skipEmptyLines, int idColumnNumber, int firstConceptRowNumber, int sheetNumber, int refSetType, String subsetName, String fileExtension,
			String effectiveTime, String namespace, String fieldSeparator, String quoteCharacter, String lineFeedCharacter, File importFile) throws SnowowlServiceException {
		this.userId = userId;
		this.branchPath = BranchPathUtils.createPath(branchPath);
		this.hasHeader = hasHeader;
		this.skipEmptyLines = skipEmptyLines;
		this.idColumnNumber = idColumnNumber;
		this.firstConceptRowNumber = firstConceptRowNumber;
		this.sheetNumber = sheetNumber;
		this.refSetType = refSetType;
		this.fileExtension = fileExtension;
		this.effectiveTime = effectiveTime;
		this.namespace = namespace;
		this.quoteCharacter = quoteCharacter;
		this.importFile = importFile;
		this.isUiImport = true;
		this.importedConceptIds = Sets.newHashSet();
		
		if (lineFeedCharacter.equals("nl")) {
			this.lineFeedCharacter = EOL.LF;
		} else {
			this.lineFeedCharacter = EOL.CRLF;
		}
		
		if (subsetName.endsWith("reference set")) {
			this.subsetName = subsetName;
		} else {
			this.subsetName = subsetName + " reference set";
		}
		
		if (fieldSeparator.equals("\\|")) {
			fieldSeparatorCharacter = '|';
		} else if (fieldSeparator.equals("")) {
			fieldSeparatorCharacter = '\0';
		} else {
			fieldSeparatorCharacter = fieldSeparator.charAt(0);
		}

	}

	/**
	 * Does the import of a reference set from the obtained file.
	 * 
	 * @return {@link SnomedUnimportedRefSets}
	 * @throws SnowowlServiceException
	 *             if there was an error during the import
	 */
	public SnomedUnimportedRefSets doImport() throws SnowowlServiceException {
		BufferedReader reader = null;
		
		try (TransactionContext context = new ImportOnlySnomedTransactionContext(new SnomedEditingContext(this.branchPath))) {
			final SubsetInformation information = createSubsetInformation();
			if (null != information.getEffectiveTime()) {
				unimportedRefSets = new SnomedUnimportedRefSets(
						importFile.getName(), 
						information.getSubsetName(), 
						information.getNameSpace(), 
						EffectiveTimes.format(information.getEffectiveTime(), DateFormats.SHORT));
			} else {
				unimportedRefSets = new SnomedUnimportedRefSets(importFile.getName(), information.getSubsetName(), information.getNameSpace(), null);
			}
			
			createHierarchy(context);
			final SubsetImporterCallback callBack = createCallBack(context, information);
			final CsvSettings csvSettings = createCVSSettings();
			
			if (fileExtension.equals("csv")) {
				CsvVariableFieldCountParser parser = new CsvVariableFieldCountParser(new File(importFile.getAbsolutePath()), csvSettings, hasHeader, skipEmptyLines);
				parser.parse();
				
				callBack.handleNonTxtFileRecord(parser.getContent());
			} else if (fileExtension.equals("xls") || fileExtension.equals("xlsx")) {
				XlsParser parser = new XlsParser(new File(importFile.getAbsolutePath()), hasHeader, skipEmptyLines);
				parser.parse(sheetNumber);
				
				callBack.handleNonTxtFileRecord(parser.getContent());
			} else if (fileExtension.equals("txt")) {
				int refSetColumnCount = getRefSetColumnCount();
				reader = new BufferedReader(new FileReader(importFile));
				CsvParser csvParser = new CsvParser(reader, csvSettings, callBack, refSetColumnCount);
				csvParser.parse();
			}

			if (null != information.getEffectiveTime()) {
				updateEffectiveTimes(context, information.getEffectiveTime());
			}

			final String comment = new StringBuilder("Imported ").append(information.getSubsetName()).append(".").toString();
			context.commit(userId, comment);
		} catch (IOException e) {
			LOGGER.error("Error while reading input file.");
			throw new SnowowlServiceException("Error while reading input file.", e);
		} catch (ParseException e) {
			LOGGER.error("Error while parsing input file.");
			throw new SnowowlServiceException("Error while parsing input file.", e);
		} catch (Exception e) {
			throw new SnowowlServiceException("Error while importing subsets.", e);
		} finally {
			Closeables.closeQuietly(reader);
		}

		return unimportedRefSets;
	}
	
	// Creates the SubsetImporterCallBack for the given RefSet type
	private SubsetImporterCallback createCallBack(TransactionContext context, SubsetInformation information) {
		final String label = information.getSubsetName();
		switch (refSetType) {
			case 0:	// place under Simple type reference set
				return new SubsetImporterCallback(label, unimportedRefSets, context, idColumnNumber, hasHeader, Concepts.REFSET_SIMPLE_TYPE);
			case 1:	 // place under B2i examples
				return new SubsetImporterCallback(label, unimportedRefSets, context, idColumnNumber, hasHeader, Concepts.REFSET_B2I_EXAMPLE);
			case 2:	// place under KP CONVERGENT MEDICAL TERMINOLOGY
				return new SubsetImporterCallback(label, unimportedRefSets, context, idColumnNumber, hasHeader, Concepts.REFSET_KP_CONVERGENT_MEDICAL_TERMINOLOGY);
			case 3:	// place under CORE PROBLEM LIST REFERENCE SETS
				return new SubsetImporterCallback(label, unimportedRefSets, context, idColumnNumber, hasHeader, Concepts.REFSET_CORE_PROBLEM_LIST_REFERENCE_SETS);
			case 4:	// place under INFOWAY PRIMARY HEALTH CARE REFERENCE SETS
				return new SubsetImporterCallback(label, unimportedRefSets, context, idColumnNumber, hasHeader, Concepts.REFSET_INFOWAY_PRIMARY_HEALTH_CARE_REFERENCE_SETS);
			default:
				return null;
		}
	}

	// Creates the hierarchy where the RefSet will be placed
	private void createHierarchy(TransactionContext context) throws CommitException {
		if (1 == refSetType) { // place under B2i examples
			createB2iExampleConcept(context);
		} else if (2 == refSetType) {  // place under KP CONVERGENT MEDICAL TERMINOLOGY
			createB2iExampleConcept(context);
			createKpConvergentMedicalTerminologyConcept(context);
		} else if (3 == refSetType) {  // place under CORE PROBLEM LIST REFERENCE SETS
			createB2iExampleConcept(context);
			createCOREProblemListConcept(context);
		} else if (4 == refSetType) {  // place under INFOWAY PRIMARY HEALTH CARE REFERENCE SETS
			createB2iExampleConcept(context);
			createInfowayPrimaryHealthCareConcept(context);
		}
	}

	// Creates the B2i examples concept
	private void createB2iExampleConcept(TransactionContext context) {
		if (null == getTerminologyBrowser().getConcept(branchPath, Concepts.REFSET_B2I_EXAMPLE)) {
			createConcept(context, Concepts.REFSET_SIMPLE_TYPE, Concepts.REFSET_B2I_EXAMPLE, Concepts.MODULE_B2I_EXTENSION, "B2i examples");
		}
	}

	// Creates the KP Convergent Medical Terminology concept
	private void createKpConvergentMedicalTerminologyConcept(TransactionContext context) {
		if (null == getTerminologyBrowser().getConcept(branchPath, Concepts.REFSET_KP_CONVERGENT_MEDICAL_TERMINOLOGY)) {
			createConcept(context, Concepts.REFSET_B2I_EXAMPLE, Concepts.REFSET_KP_CONVERGENT_MEDICAL_TERMINOLOGY, Concepts.MODULE_B2I_EXTENSION,
					"KP Convergent Medical Terminology");
		}
	}
	
	// Creates the CORE Problem List concept
	private void createCOREProblemListConcept(TransactionContext context) {
		if (null == getTerminologyBrowser().getConcept(branchPath, Concepts.REFSET_CORE_PROBLEM_LIST_REFERENCE_SETS)) {
			createConcept(context, Concepts.REFSET_B2I_EXAMPLE, Concepts.REFSET_CORE_PROBLEM_LIST_REFERENCE_SETS, Concepts.MODULE_B2I_EXTENSION,
					"CORE Problem List Reference Sets");
		}
	}

	// Creates the Infoway Primary Health Care concept
	private void createInfowayPrimaryHealthCareConcept(TransactionContext context) throws CommitException {
		if (null == getTerminologyBrowser().getConcept(branchPath, Concepts.REFSET_INFOWAY_PRIMARY_HEALTH_CARE_REFERENCE_SETS)) {
			createConcept(context, Concepts.REFSET_B2I_EXAMPLE, Concepts.REFSET_INFOWAY_PRIMARY_HEALTH_CARE_REFERENCE_SETS, Concepts.MODULE_B2I_EXTENSION,
					"Infoway Primary Health Care Reference Sets");
		}
	}

	// Sets the CVS settings
	private CsvSettings createCVSSettings() {
		return new CsvSettings("".equals(quoteCharacter) ? '\0' : quoteCharacter.charAt(0), fieldSeparatorCharacter, lineFeedCharacter, true);
	}

	// Creates the SubsetInformtaion
	private SubsetInformation createSubsetInformation() throws ParseException {
		SubsetInformation subsetInformation = new SubsetInformation(importFile.getName());
		if (isUiImport) {
			subsetInformation.setSubsetName(subsetName);
			if (!CompareUtils.isEmpty(namespace)) {
				subsetInformation.setNameSpace(namespace);
			} else {
				// default namespace is 1000154
				subsetInformation.setNameSpace(Concepts.B2I_NAMESPACE);
			}
			if (!CompareUtils.isEmpty(effectiveTime)) {
				subsetInformation.setEffectiveTime(EffectiveTimes.parse(effectiveTime, DateFormats.SHORT));
			}
		} else {
			subsetInformation.parse();
		}
		return subsetInformation;
	}

	// Updates the effective time
	private void updateEffectiveTimes(TransactionContext context, Date effectiveTime) {
		final SnomedEditingContext editingContext = context.service(SnomedEditingContext.class);
		for (final Component item : editingContext.getNewObjects(Component.class)) {
			item.setEffectiveTime(effectiveTime);
			item.setReleased(true);
		}
		for (final SnomedRefSetMember member : editingContext.getNewObjects(SnomedRefSetMember.class)) {
			member.setEffectiveTime(effectiveTime);
			member.setReleased(true);
		}
	}

	// Creates a concept
	private void createConcept(final TransactionContext context, final String parentConceptId, final String conceptId, final String moduleId, final String label) {
		// TODO remove lang refset ID from here, and use hard coded one for these custom concepts to be reproducible
		final String languageRefSetId = context.service(SnomedEditingContext.class).getLanguageRefSetId();
		final String createdConceptId = SnomedRequests
			.prepareNewConcept()
			.setId(conceptId)
			.setModuleId(moduleId)
			.setParent(parentConceptId)
			.setIsAId(new ReservingIdStrategy(ComponentCategory.RELATIONSHIP, Concepts.B2I_NAMESPACE))
			.addDescription(SnomedRequests
					.prepareNewDescription()
					.setIdFromNamespace(Concepts.B2I_NAMESPACE)
					.setModuleId(moduleId)
					.setTerm(label)
					.setTypeId(Concepts.FULLY_SPECIFIED_NAME)
					.preferredIn(languageRefSetId))
			.addDescription(SnomedRequests
					.prepareNewDescription()
					.setIdFromNamespace(Concepts.B2I_NAMESPACE)
					.setModuleId(moduleId)
					.setTerm(label)
					.setTypeId(Concepts.SYNONYM)
					.preferredIn(languageRefSetId))
			.build()
			.execute(context);
		
		SnomedRequests
			.prepareNewRelationship()
			.setIdFromNamespace(Concepts.B2I_NAMESPACE)
			.setModuleId(moduleId)
			.setSourceId(createdConceptId)
			.setDestinationId(parentConceptId)
			.setTypeId(Concepts.IS_A)
			.setCharacteristicType(CharacteristicType.INFERRED_RELATIONSHIP)
			.build()
			.execute(context);
		
		context.commit(userId, "Created '" + label + "' concept");
	}

	/*
	 * Count the columns of the import file.
	 * 
	 * @return the number of the columns
	 * 
	 * @throws SnowowlServiceException
	 */
	private int getRefSetColumnCount() throws SnowowlServiceException {
		int columnCount = 0;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(importFile));
			
			char[] charArray = new char[1024];
			reader.read(charArray, 0, 1024);
			
			boolean isNewLine = false;
			for (char c : charArray) {
				if (!isNewLine) {
					if (c == fieldSeparatorCharacter && !isNewLine) {
						columnCount++;
					} else if (c == '\r' || c == '\n') {
						columnCount++;
						isNewLine = true;
					}
				}
			}
			
			return columnCount;
		} catch (IOException e) {
			LOGGER.error("Error while counting columns.");
			throw new SnowowlServiceException("Error while reading input file.", e);
		} finally {
			Closeables.closeQuietly(reader);
		}
	}
	
	protected ICDOConnection getCDOConnection() {
		return ApplicationContext.getInstance().getService(ICDOConnectionManager.class).get(SnomedPackage.eINSTANCE);
	}
	
	private ITerminologyBrowser<SnomedConceptIndexEntry, String> getTerminologyBrowser() {
		return ApplicationContext.getInstance().getService(SnomedTerminologyBrowser.class);
	}
	
	/*
	 * Class which will creates the reference set and extracted the members from
	 * the import file rows and adds them to the reference set.
	 */
	private final class SubsetImporterCallback implements RecordParserCallback<String> {

		private final Map<String, String> CMT_NAME_ID_PAIRS = ImmutableMap.<String, String>builder()
				.put("Cardiology", Concepts.CARDIOLOGY_REFERENCE_SET)
				.put("Endocrinology Urology Nephrology", Concepts.ENDOCRINOLOGY_UROLOGY_NEPHROLOGY_REFERENCE_SET)
				.put("Hematology Oncology", Concepts.HEMATOLOGY_ONCOLOGY_REFERENCE_SET)
				.put("Mental Health", Concepts.MENTAL_HEALTH_REFERENCE_SET)
				.put("Musculoskeletal", Concepts.MUSCULOSKELETAL_REFERENCE_SET)
				.put("Neurology", Concepts.NEUROLOGY_REFERENCE_SET)
				.put("Ophthalmology", Concepts.OPHTHALMOLOGY_REFERENCE_SET)
				.put("ENT Gastrointestinal Infectious Diseases", Concepts.ENT_GASTROINTESTINAL_INFECTIOUS_DISEASES_REFERENCE_SET)
				.put("Hx of and FHx of", Concepts.HX_OF_AND_FHX_OF_REFERENCE_SET)
				.put("Injuries [Part 1]", Concepts.INJURIES_PART_1_REFERENCE_SET)
				.put("Obstetrics and Gynecology", Concepts.OBSTETRICS_AND_GYNECOLOGY_REFERENCE_SET)
				.put("Orthopedics Extremity Fractures", Concepts.ORTHOPEDICS_EXTREMITY_FRACTURES_REFERENCE_SET)
				.put("Orthopedics Non-Extremity Fractures", Concepts.ORTHOPEDICS_NON_EXTREMITY_FRACTURES_REFERENCE_SET)
				.put("Primary Care", Concepts.PRIMARY_CARE_REFERENCE_SET)
				.put("Skin Respiratory", Concepts.SKIN_RESPIRATORY_REFERENCE_SET)
				.put("KP Problem List", Concepts.KP_PROBLEM_LIST_REFERENCE_SET)
				.build();
		
		private final SnomedUnimportedRefSets unImportedRefSets;
		private String moduleId;
		private final int idColumnNumber;
		private final boolean hasHeader;
		private final String refSetId;
		private final TransactionContext context;

		private SubsetImporterCallback(String label, SnomedUnimportedRefSets unimportedRefSet, TransactionContext context,
				int idColumnNumber, boolean hasHeader, String refSetType) {
			this.unImportedRefSets = unimportedRefSet;
			this.context = context;
			this.idColumnNumber = idColumnNumber;
			this.hasHeader = hasHeader; 
			this.moduleId = context.lookup(refSetType, Concept.class).getModule().getId();
			final String languageReferenceSetId = context.service(SnomedEditingContext.class).getLanguageRefSetId();
			
			SnomedConceptCreateRequestBuilder identifierConceptReq = SnomedRequests
					.prepareNewConcept()
					.setModuleId(moduleId)
					.setParent(refSetType)
					.setIsAId(new ReservingIdStrategy(ComponentCategory.RELATIONSHIP, Concepts.B2I_NAMESPACE))
					.addDescription(SnomedRequests
							.prepareNewDescription()
							.setIdFromNamespace(Concepts.B2I_NAMESPACE)
							.setModuleId(moduleId)
							.setTerm(label)
							.setTypeId(Concepts.FULLY_SPECIFIED_NAME)
							.preferredIn(languageReferenceSetId))
					.addDescription(SnomedRequests
							.prepareNewDescription()
							.setIdFromNamespace(Concepts.B2I_NAMESPACE)
							.setModuleId(moduleId)
							.setTerm(label)
							.setTypeId(Concepts.SYNONYM)
							.preferredIn(languageReferenceSetId));
			
			// replace ID if it is a known refset
			final String cmtRefSetId = getIdIfCMTConcept(label);
			if (cmtRefSetId == null) {
				// XXX hardcoded B2i namespace???
				identifierConceptReq.setIdFromNamespace(Concepts.B2I_NAMESPACE);
			} else {
				identifierConceptReq.setId(cmtRefSetId);
			}
			
			this.refSetId = SnomedRequests
				.prepareNewRefSet()
				.setType(SnomedRefSetType.SIMPLE)
				.setReferencedComponentType(SnomedTerminologyComponentConstants.CONCEPT)
				.setIdentifierConcept(identifierConceptReq)
				.build()
				.execute(context);
			
			// We create an inferred ISA manually to the same parent
			SnomedRequests.prepareNewRelationship()
				.setIdFromNamespace(Concepts.B2I_NAMESPACE)
				.setModuleId(moduleId)
				.setSourceId(refSetId)
				.setDestinationId(refSetType)
				.setTypeId(Concepts.IS_A)
				.setCharacteristicType(CharacteristicType.INFERRED_RELATIONSHIP)
				.build().execute(context);
		}
		
		// update the concept Id to default constant id if the concept is CMT concept
		private String getIdIfCMTConcept(String label) {
			return CMT_NAME_ID_PAIRS.get(label.replaceAll(" reference set", ""));
		}

		@Override
		public void handleRecord(int recordCount, List<String> record) {
			if (hasHeader) {
				if (firstConceptRowNumber < recordCount) {
					createMember(record);
				}
			} else {
				createMember(record);
			}
		}
		
		public void handleNonTxtFileRecord(List<List<String>> content) {
			for (List<String> rowsList : content) {
				SnomedConceptIndexEntry concept = getConcept(rowsList.get(idColumnNumber));
				if (concept != null && concept.isActive() && importedConceptIds.add(concept.getId())) {
					createMember(concept);
				} else {
					createUnimportedRefsetMember(rowsList, concept);
				}
			}
		}
		
		private void createMember(List<String> record) {
			SnomedConceptIndexEntry concept = getConcept(record.get(idColumnNumber));
			if (concept != null && concept.isActive() && importedConceptIds.add(concept.getId())) {
				createMember(concept);
			} else {
				createUnimportedRefsetMember(record, concept);
			}
		}
		
		private void createUnimportedRefsetMember(List<String> rowsList, SnomedConceptIndexEntry concept) {
			String identifier;
			if (null != getFullySpecifiedName(rowsList)) {
				 identifier = getFullySpecifiedName(rowsList);
			} else {
				identifier = rowsList.get(idColumnNumber) ;
			}
			
			if (concept == null) {
				unImportedRefSets.addRefSetMember("Not found", rowsList.get(idColumnNumber), identifier);
			} else if (!importedConceptIds.add(concept.getId())) {
				unImportedRefSets.addRefSetMember("Duplicated", rowsList.get(idColumnNumber), identifier);
			} else {
				unImportedRefSets.addRefSetMember("Inactive", rowsList.get(idColumnNumber), identifier);
			}
		}
		
		private String getFullySpecifiedName(List<String> rowsList) {
			for (String value : rowsList) {
				if (value.contains("(") && value.contains(")")) {
					return value;
				}
			}
			return null;
		}

		private void createMember(SnomedConceptIndexEntry concept) {
			SnomedRequests.prepareNewMember()
				.setModuleId(moduleId)
				.setReferencedComponentId(concept.getId())
				.setReferenceSetId(refSetId)
				.build()
				.execute(context);
		}

		private SnomedConceptIndexEntry getConcept(final String conceptId) {
			if (conceptId.matches("\\d+")) {
				return getTerminologyBrowser().getConcept(branchPath, conceptId);
			} else {
				return null;
			}
		}
	}
}