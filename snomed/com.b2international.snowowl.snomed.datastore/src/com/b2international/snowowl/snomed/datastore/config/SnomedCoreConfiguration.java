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
package com.b2international.snowowl.snomed.datastore.config;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.b2international.snowowl.datastore.config.ConnectionPoolConfiguration;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SNOMED CT related application level configuration parameters.
 * 
 * @since 3.4
 */
public class SnomedCoreConfiguration extends ConnectionPoolConfiguration {
	
	public static final String ELK_REASONER_ID = "org.semanticweb.elk.elk.reasoner.factory"; //$NON-NLS-1$
	private static final String DEFAULT_REASONER = ELK_REASONER_ID;
	public static final String DEFAULT_LANGUAGE = "en-gb"; //$NON-NLS-1$
	public static final int DEFAULT_MAXIMUM_REASONER_COUNT = 2;
	public static final int DEFAULT_MAXIMUM_REASONER_RESULTS = 10;
	public static final String DEFAULT_NAMESPACE = ""; //$NON-NLS-1$
	public static final String DEFAULT_MODULE = Concepts.MODULE_SCT_CORE;
	
	@Min(1)
	@Max(3)
	private int maxReasonerCount = DEFAULT_MAXIMUM_REASONER_COUNT;
	
	@Min(1)
	@Max(100)
	private int maxReasonerResults = DEFAULT_MAXIMUM_REASONER_RESULTS;
	
	@NotEmpty
	private String defaultReasoner = DEFAULT_REASONER;
	
	@NotEmpty
	private String language = DEFAULT_LANGUAGE;
	
	@NotNull
	private String defaultNamespace = DEFAULT_NAMESPACE;
	
	@NotEmpty
	private String concreteDomainTypeRefsetIdentifier = Concepts.REFSET_CONCRETE_DOMAIN_TYPE;
	
	@NotEmpty
	private String stringDatatypeRefsetIdentifier = Concepts.REFSET_STRING_DATATYPE;
	
	@NotEmpty
	private String booleanDatatypeRefsetIdentifier = Concepts.REFSET_BOOLEAN_DATATYPE;
	
	@NotEmpty
	private String floatDatatypeRefsetIdentifier = Concepts.REFSET_FLOAT_DATATYPE;
	
	@NotEmpty
	private String integerDatatypeRefsetIdentifier = Concepts.REFSET_INTEGER_DATATYPE;
	
	@NotEmpty
	private String datetimeDatatypeRefsetIdentifier = Concepts.REFSET_DATETIME_DATATYPE;
	
	@NotEmpty
	private String defaultModule = DEFAULT_MODULE;
	
	@Valid
	private SnomedIdentifierConfiguration ids = new SnomedIdentifierConfiguration();
	
	private boolean collectSystemChanges = false;
	
	private boolean concreteDomainSupport = false;
	
	private boolean showReasonerUsageWarning = true;
	
	//enables the manual editing of inferred relationships and concrete data types
	private boolean inferredEditingEnabled = false;
		
	/**
	 * @return the maxReasonerCount
	 */
	@JsonProperty
	public int getMaxReasonerCount() {
		return maxReasonerCount;
	}
	
	/**
	 * @param maxReasonerCount the maxReasonerCount to set
	 */
	@JsonProperty
	public void setMaxReasonerCount(int maxReasonerCount) {
		this.maxReasonerCount = maxReasonerCount;
	}
	
	/**
	 * @return the maxReasonerResults
	 */
	@JsonProperty
	public int getMaxReasonerResults() {
		return maxReasonerResults;
	}
	
	/**
	 * @param maxReasonerResults the maxReasonerResults to set
	 */
	@JsonProperty
	public void setMaxReasonerResults(int maxReasonerResults) {
		this.maxReasonerResults = maxReasonerResults;
	}
	
	/**
	 * @return the language code currently used for SNOMED CT
	 */
	@JsonProperty
	public String getLanguage() {
		return language;
	}
	
	/**
	 * @return the default namespace used for SNOMED CT
	 */
	@JsonProperty
	public String getDefaultNamespace() {
		return defaultNamespace;
	}
	
	/**
	 * @param defaultNamespace the default namespace to set
	 */
	@JsonProperty
	public void setDefaultNamespace(String defaultNamespace) {
		this.defaultNamespace = defaultNamespace;
	}
	
	/**
	 * @return the default module user for SNOMED CT
	 */
	@JsonProperty
	public String getDefaultModule() {
		return defaultModule;
	}
	
	/**
	 * @param defaultModule the default module to set
	 */
	@JsonProperty
	public void setDefaultModule(String defaultModule) {
		this.defaultModule = defaultModule;
	}
	
	/**
	 * @param language the SNOMED CT language code to set
	 */
	@JsonProperty
	public void setLanguage(String language) {
		this.language = language;
	}
	
	/**
	 * @return the currently set default reasoner ID 
	 */
	@JsonProperty
	public String getDefaultReasoner() {
		return defaultReasoner;
	}
	
	/**
	 * @param defaultReasoner - the reasoner to set as default
	 */
	@JsonProperty
	public void setDefaultReasoner(String defaultReasoner) {
		this.defaultReasoner = defaultReasoner;
	}

	@JsonProperty("concreteDomainSupport")
	public boolean isConcreteDomainSupported() {
		return concreteDomainSupport;
	}
	
	@JsonProperty("concreteDomainSupport")
	public void setConcreteDomainSupported(boolean concreteDomainSupport) {
		this.concreteDomainSupport = concreteDomainSupport;
	}

	@JsonProperty("showReasonerUsageWarning")
	public boolean isShowReasonerUsageWarningEnabled() {
		return showReasonerUsageWarning ;
	}
	
	@JsonProperty("showReasonerUsageWarning")
	public void setShowReasonerUsageWarningEnabled(boolean showReasonerUsageWarning) {
		this.showReasonerUsageWarning = showReasonerUsageWarning;
	}
	
	@JsonProperty("inferredEditingEnabled")
	public boolean isInferredEditingEnabled() {
		return inferredEditingEnabled;
	}

	@JsonProperty("inferredEditingEnabled")
	public void setInferredEditingEnabled(boolean inferredEditingEnabled) {
		this.inferredEditingEnabled = inferredEditingEnabled;
	}
	
	@JsonProperty("collectSystemChanges")
	public boolean isCollectSystemChanges() {
		return collectSystemChanges;
	}
	
	@JsonProperty("collectSystemChanges")
	public void setCollectSystemChanges(boolean collectSystemChanges) {
		this.collectSystemChanges = collectSystemChanges;
	}
	
	public SnomedIdentifierConfiguration getIds() {
		return ids;
	}
	
	public void setIds(SnomedIdentifierConfiguration ids) {
		this.ids = ids;
	}

	/**
	 * The ID of the concrete domain type reference set identifier concept
	 * 
	 * @return the concreteDomainTypeRefsetIdentifier
	 */
	@JsonProperty("concreteDomainTypeRefsetIdentifier")
	public String getConcreteDomainTypeRefsetIdentifier() {
		return concreteDomainTypeRefsetIdentifier;
	}

	/**
	 * Sets the ID of the concrete domain type reference set identifier concept
	 * 
	 * @param concreteDomainTypeRefsetIdentifier the concreteDomainTypeRefsetIdentifier to set
	 */
	@JsonProperty("concreteDomainTypeRefsetIdentifier")
	public void setConcreteDomainTypeRefsetIdentifier(String concreteDomainTypeRefsetIdentifier) {
		this.concreteDomainTypeRefsetIdentifier = concreteDomainTypeRefsetIdentifier;
	}

	/**
	 * The ID of the string datatype reference set identifier concept
	 * 
	 * @return the stringDatatypeRefsetIdentifier
	 */
	@JsonProperty("stringDataTypeRefsetIdentifier")
	public String getStringDatatypeRefsetIdentifier() {
		return stringDatatypeRefsetIdentifier;
	}

	/**
	 * Sets the ID of the string datatype reference set identifier concept
	 * 
	 * @param stringDatatypeRefsetIdentifier the stringDatatypeRefsetIdentifier to set
	 */
	@JsonProperty("stringDataTypeRefsetIdentifier")
	public void setStringDatatypeRefsetIdentifier(String stringDatatypeRefsetIdentifier) {
		this.stringDatatypeRefsetIdentifier = stringDatatypeRefsetIdentifier;
	}

	/**
	 * The ID of the boolean datatype reference set identifier concept
	 * 
	 * @return the booleanDatatypeRefsetIdentifier
	 */
	@JsonProperty("booleanDataTypeRefsetIdentifier")
	public String getBooleanDatatypeRefsetIdentifier() {
		return booleanDatatypeRefsetIdentifier;
	}

	/**
	 * Sets the ID of the boolean datatype reference set identifier concept
	 * 
	 * @param booleanDatatypeRefsetIdentifier the booleanDatatypeRefsetIdentifier to set
	 */
	@JsonProperty("booleanDataTypeRefsetIdentifier")
	public void setBooleanDatatypeRefsetIdentifier(String booleanDatatypeRefsetIdentifier) {
		this.booleanDatatypeRefsetIdentifier = booleanDatatypeRefsetIdentifier;
	}

	/**
	 * The ID of the float datatype reference set identifier concept
	 * 
	 * @return the floatDatatypeRefsetIdentifier
	 */
	@JsonProperty("floatDataTypeRefsetIdentifier")
	public String getFloatDatatypeRefsetIdentifier() {
		return floatDatatypeRefsetIdentifier;
	}

	/**
	 * Sets the ID of the float datatype reference set identifier concept
	 * 
	 * @param floatDatatypeRefsetIdentifier the floatDatatypeRefsetIdentifier to set
	 */
	@JsonProperty("floatDataTypeRefsetIdentifier")
	public void setFloatDatatypeRefsetIdentifier(String floatDatatypeRefsetIdentifier) {
		this.floatDatatypeRefsetIdentifier = floatDatatypeRefsetIdentifier;
	}

	/**
	 * The ID of the integer datatype reference set identifier concept
	 * 
	 * @return the integerDatatypeRefsetIdentifier
	 */
	@JsonProperty("integerDataTypeRefsetIdentifier")
	public String getIntegerDatatypeRefsetIdentifier() {
		return integerDatatypeRefsetIdentifier;
	}

	/**
	 * Sets the ID of the integer datatype reference set identifier concept
	 * 
	 * @param integerDatatypeRefsetIdentifier the integerDatatypeRefsetIdentifier to set
	 */
	@JsonProperty("integerDataTypeRefsetIdentifier")
	public void setIntegerDatatypeRefsetIdentifier(String integerDatatypeRefsetIdentifier) {
		this.integerDatatypeRefsetIdentifier = integerDatatypeRefsetIdentifier;
	}

	/**
	 * The ID of the datetime datatype reference set identifier concept
	 * 
	 * @return the datetimeDatatypeRefsetIdentifier
	 */
	@JsonProperty("datetimeDataTypeRefsetIdentifier")
	public String getDatetimeDatatypeRefsetIdentifier() {
		return datetimeDatatypeRefsetIdentifier;
	}

	/**
	 * Sets the ID of the datetime datatype reference set identifier concept
	 * 
	 * @param datetimeDatatypeRefsetIdentifier the datetimeDatatypeRefsetIdentifier to set
	 */
	@JsonProperty("datetimeDataTypeRefsetIdentifier")
	public void setDatetimeDatatypeRefsetIdentifier(String datetimeDatatypeRefsetIdentifier) {
		this.datetimeDatatypeRefsetIdentifier = datetimeDatatypeRefsetIdentifier;
	}
}