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
package com.b2international.snowowl.snomed.datastore.services;

import java.util.List;
import java.util.Set;

import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.common.commit.CDOChangeSetData;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.CDOIDAndVersion;
import org.eclipse.emf.cdo.common.revision.CDORevisionKey;
import org.eclipse.emf.cdo.common.revision.delta.CDOFeatureDelta;
import org.eclipse.emf.cdo.common.revision.delta.CDOSetFeatureDelta;
import org.eclipse.emf.cdo.transaction.CDOTransaction;

import bak.pcj.LongIterator;
import bak.pcj.set.LongSet;

import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.datastore.cdo.CDOUtils;
import com.b2international.snowowl.snomed.Component;
import com.b2international.snowowl.snomed.SnomedPackage;
import com.b2international.snowowl.snomed.datastore.SnomedClientRefSetBrowser;
import com.b2international.snowowl.snomed.snomedrefset.SnomedModuleDependencyRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetPackage;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

/**
 * Stateless service class for updating existing {@link SnomedModuleDependencyRefSetMember module dependency member's}
 * source and target effective time before committing the transaction.
 * 
 * @since Snow&nbsp;Owl 3.0
 */
public class SnomedModuleDependencyRefSetService {
	
	/**
	 * Updates the module dependency reference set members based on the changes.
	 * It should be called before committing the transaction.
	 * @param transaction the underlying transaction for the SNOMED&nbsp;CT components.
	 */
	public void updateModuleDependenciesDuringPreCommit(final CDOTransaction transaction) {

		// We ignore those cases when a new module is created or a component has moved to a module
		// which hasn't got a MDR member. These cases will be handled at the export as it requires
		// more calculation
		
		final Set<String> moduleIds = Sets.newHashSet();
		final CDOChangeSetData changeSetData = transaction.getChangeSetData();
		final SnomedClientRefSetBrowser browser = ApplicationContext.getInstance().getService(SnomedClientRefSetBrowser.class);
		
		// collect the module IDs to update
		moduleIds.addAll(getModuleIdsFromNewObjects(changeSetData.getNewObjects(), transaction));
		moduleIds.addAll(getModuleIdsFromDetachedObjects(changeSetData.getDetachedObjects(), transaction));
		moduleIds.addAll(getModuleIdsFromChangedObjects(changeSetData.getChangedObjects(), transaction));
		
		// search for members where the module/referenced component ID equals to the collected ID
		// and the source/target effective time is set
		for (final String moduleId : moduleIds) {
			final LongSet storageKeys = browser.getPublishedModuleDependencyMembers(moduleId);
			
			for (final LongIterator iterator = storageKeys.iterator(); iterator.hasNext();) {
				final long storageKey = iterator.next();
				final CDOObject cdoObject = CDOUtils.getObjectIfExists(transaction, storageKey);
				if (null != cdoObject && cdoObject instanceof SnomedModuleDependencyRefSetMember) {
					final SnomedModuleDependencyRefSetMember member = (SnomedModuleDependencyRefSetMember) cdoObject;
					
					if (member.getModuleId().equals(moduleId)) {
						member.setSourceEffectiveTime(null);
					} else if (member.getReferencedComponentId().equals(moduleId)) {
						member.setTargetEffectiveTime(null);
					}
					
					member.unsetEffectiveTime();
				}
			}
		}
	}
	
	/*
	 * Collects the module IDs from the new objects.
	 * Precommit state.
	 */
	private Set<String> getModuleIdsFromNewObjects(final List<CDOIDAndVersion> objects, final CDOTransaction cdoTransaction) {
		final Set<String> moduleIds = Sets.newHashSet();
		
		for (final CDOIDAndVersion cdoidAndVersion : objects) {
			final CDOObject object = cdoTransaction.getObject(cdoidAndVersion.getID());
			final String moduleId;
			if (object instanceof Component) {
				moduleId = ((Component) object).getModule().getId();
			} else if (object instanceof SnomedRefSetMember) {
				moduleId = ((SnomedRefSetMember) object).getModuleId();
			} else  {
				continue;
			}
			
			moduleIds.add(moduleId);
		}
		
		return moduleIds;
	}
	
	/*
	 * Collects the module IDs from the detached objects.
	 * Precommit state.
	 */
	private Set<String> getModuleIdsFromDetachedObjects(final List<CDOIDAndVersion> objects, final CDOTransaction cdoTransaction) {
		final Set<String> moduleIds = Sets.newHashSet();
		
		for (final CDOIDAndVersion cdoidAndVersion : objects) {
			final String moduleId;
			final Object moduleValue;
			final CDOObject object = cdoTransaction.getObject(cdoidAndVersion.getID());
			
			if (object instanceof Component) {
				final Component component = (Component) object;
				moduleValue = CDOUtils.getAttribute(component, SnomedPackage.eINSTANCE.getComponent_Module(), Object.class);
			} else if (object instanceof SnomedRefSetMember) {
				final SnomedRefSetMember member = (SnomedRefSetMember) object;
				moduleValue = CDOUtils.getAttribute(member, SnomedRefSetPackage.eINSTANCE.getSnomedRefSetMember_ModuleId(), Object.class);
			} else {
				continue;
			}
			
			if (moduleValue instanceof CDOID) {
				moduleId = CDOUtils.getAttribute(cdoTransaction.getObject((CDOID) moduleValue), SnomedPackage.eINSTANCE.getComponent_Id(), String.class);
			} else if (moduleValue instanceof CDOObject) {
				moduleId = CDOUtils.getAttribute((CDOObject) moduleValue, SnomedPackage.eINSTANCE.getComponent_Id(), String.class);
			} else if (moduleValue instanceof String) {
				moduleId = String.valueOf(moduleValue);
			} else {
				throw new IllegalStateException("Couldn't determine module ID for detached object.");
			}
			
			Preconditions.checkNotNull(moduleId, CDOUtils.toString(cdoTransaction));
			moduleIds.add(moduleId);
			
			// XXX What if the user deleted or moved the last component which belonged to a module?
			// Should we delete the related module dependency refset members too?
		}
		
		return  moduleIds;
	}

	/*
	 * Collects the module IDs from the changed objects.
	 * Precommit state.
	 */
	private Set<String> getModuleIdsFromChangedObjects(final List<CDORevisionKey> changedObjects, final CDOTransaction cdoTransaction) {
		final Set<String> moduleIds = Sets.newHashSet();
		
		for (final CDOIDAndVersion cdoidAndVersion : changedObjects) {
			final String moduleId;
			final CDOObject object = cdoTransaction.getObject(cdoidAndVersion.getID());
			
			if (object instanceof Component) {
				moduleId = ((Component) object).getModule().getId();
			} else if (object instanceof SnomedRefSetMember) {
				moduleId = ((SnomedRefSetMember) object).getModuleId();
			} else {
				continue;
			}
			
			moduleIds.add(moduleId);

			// we need to check those cases when a component is moved from one module to another
			final CDOFeatureDelta featureDelta = cdoTransaction.getRevisionDeltas().get(cdoidAndVersion.getID()).getFeatureDelta(SnomedPackage.eINSTANCE.getComponent_Module());
			if (featureDelta instanceof CDOSetFeatureDelta) {

				final CDOSetFeatureDelta cdoSetFeatureDelta = (CDOSetFeatureDelta) featureDelta;
				final Object oldValue = cdoSetFeatureDelta.getOldValue();

				final String oldModuleId;

				if (oldValue instanceof CDOID) {
					final CDOObject cdoObject = cdoTransaction.getObject((CDOID) oldValue);
					Preconditions.checkNotNull(cdoObject, CDOUtils.toString(cdoTransaction));
					
					if (cdoObject instanceof Component) {
						oldModuleId = ((Component) cdoObject).getModule().getId();
					} else if (cdoObject instanceof SnomedRefSetMember) {
						oldModuleId = ((SnomedRefSetMember) cdoObject).getModuleId();
					} else {
						continue;
					}
				} else if (oldValue instanceof CDOObject) {
					if (oldValue instanceof Component) {
						oldModuleId = ((Component) oldValue).getId();
					} else if (oldValue instanceof SnomedRefSetMember) {
						oldModuleId = ((SnomedRefSetMember) oldValue).getModuleId();
					} else {
						continue;
					}
				} else {
					throw new IllegalStateException("Couldn't determine module concept for changed object.");
				}
				
				moduleIds.add(oldModuleId);
			}
		}
		
		return moduleIds;
	}

}