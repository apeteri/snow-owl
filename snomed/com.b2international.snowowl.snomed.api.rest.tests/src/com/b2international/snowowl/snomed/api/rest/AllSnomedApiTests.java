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
package com.b2international.snowowl.snomed.api.rest;

import org.junit.ClassRule;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.b2international.commons.platform.PlatformUtil;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.api.rest.branches.SnomedBranchingApiFeature;
import com.b2international.snowowl.snomed.api.rest.branches.SnomedMergeApiFeature;
import com.b2international.snowowl.snomed.api.rest.components.SnomedConceptApiFeature;
import com.b2international.snowowl.snomed.api.rest.components.SnomedRelationshipApiFeature;
import com.b2international.snowowl.snomed.api.rest.id.SnomedIdentifierApiFeature;
import com.b2international.snowowl.snomed.api.rest.io.SnomedImportApiExamplesFeature;
import com.b2international.snowowl.snomed.api.rest.io.SnomedImportApiFeature;
import com.b2international.snowowl.snomed.common.ContentSubType;
import com.b2international.snowowl.test.commons.BundleStartRule;
import com.b2international.snowowl.test.commons.Resources;
import com.b2international.snowowl.test.commons.SnomedContentRule;
import com.b2international.snowowl.test.commons.SnowOwlAppRule;

/**
 * @since 1.0
 */
@RunWith(Suite.class)
@SuiteClasses({ 
	SnomedBranchingApiFeature.class,
	SnomedMergeApiFeature.class,
	SnomedConceptApiFeature.class,
	SnomedRelationshipApiFeature.class,
	SnomedIdentifierApiFeature.class,
	SnomedImportApiFeature.class,
	SnomedImportApiExamplesFeature.class
})
public class AllSnomedApiTests {

	@ClassRule
	public static final RuleChain appRule = RuleChain
			.outerRule(SnowOwlAppRule.snowOwl().clearResources(true).config(PlatformUtil.toAbsolutePath(AllSnomedApiTests.class, "rest-configuration.yml")))
			.around(new BundleStartRule("com.b2international.snowowl.snomed.api.rest"))
			.around(new SnomedContentRule(Resources.Snomed.MINI_RF2_INT, Concepts.REFSET_LANGUAGE_TYPE_UK, ContentSubType.FULL));
	
}