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
package com.b2international.snowowl.datastore.server.snomed.index;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.api.SnowowlServiceException;
import com.b2international.snowowl.datastore.ICDOChangeProcessor;
import com.b2international.snowowl.datastore.server.CDOChangeProcessorFactory;
import com.b2international.snowowl.datastore.server.snomed.index.init.ImportIndexServerService;

/**
 * CDO change processor factory responsible to create {@link SnomedCDOChangeProcessor change processors} for SNOMED CT terminology.
 */
public class SnomedCDOChangeProcessorFactory implements CDOChangeProcessorFactory {

	private static final String FACTORY_NAME = "SNOMED CT change processor factory";
	static final ExecutorService CHANGE_PROC_EXECUTOR = Executors.newFixedThreadPool(8);

	@Override
	public ICDOChangeProcessor createChangeProcessor(final IBranchPath branchPath) throws SnowowlServiceException {
		
		//SNOMED CT import is in progress
		if (ApplicationContext.getInstance().exists(ImportIndexServerService.class)) {
			return new SnomedImportCDOChangeProcessor(ApplicationContext.getInstance().getService(ImportIndexServerService.class), branchPath); 
		}
		
		final SnomedIndexUpdater indexService = ApplicationContext.getInstance().getService(SnomedIndexUpdater.class);
		return new SnomedCDOChangeProcessor(CHANGE_PROC_EXECUTOR, indexService, branchPath);
	}

	@Override
	public String getFactoryName() {
		return FACTORY_NAME;
	}
}
