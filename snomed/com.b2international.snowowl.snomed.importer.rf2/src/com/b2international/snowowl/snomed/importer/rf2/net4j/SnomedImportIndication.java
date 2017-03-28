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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import org.eclipse.net4j.signal.IndicationWithMonitoring;
import org.eclipse.net4j.util.io.ExtendedDataInputStream;
import org.eclipse.net4j.util.io.ExtendedDataOutputStream;
import org.eclipse.net4j.util.io.IORuntimeException;
import org.eclipse.net4j.util.io.IOUtil;
import org.eclipse.net4j.util.om.monitor.OMMonitor;
import org.eclipse.net4j.util.om.monitor.OMMonitor.Async;

import com.b2international.commons.ConsoleProgressMonitor;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.snomed.common.ContentSubType;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptIndexEntry;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRefSetIndexEntry;
import com.b2international.snowowl.snomed.importer.net4j.*;
import com.b2international.snowowl.snomed.importer.net4j.ImportConfiguration.ImportSourceKind;
import com.b2international.snowowl.snomed.importer.rf2.util.ImportUtil;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

/**
 * 
 */
public class SnomedImportIndication extends IndicationWithMonitoring {

	private interface FileCallback {
		public void setFile(File result);
	}
	
	private final ImportConfiguration importConfiguration = new ImportConfiguration();
	
	private String userId;

	private File receivedFilesDirectory;
	
	/**
	 * 
	 * @param protocol
	 */
	public SnomedImportIndication(final SnomedImportServerProtocol protocol) {
		super(protocol, SnomedImportProtocolConstants.SIGNAL_IMPORT_RF2);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.net4j.signal.IndicationWithMonitoring#getIndicatingWorkPercent()
	 */
	@Override protected int getIndicatingWorkPercent() {
		return 10;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.net4j.signal.IndicationWithMonitoring#indicating(org.eclipse.net4j.util.io.ExtendedDataInputStream, org.eclipse.net4j.util.om.monitor.OMMonitor)
	 */
	@Override protected void indicating(final ExtendedDataInputStream in, final OMMonitor monitor) throws Exception {

		monitor.begin(1 + 7 + 1);
		OMMonitor refSetSubmonitor = null;
		
		try {
			
			final IBranchPath branchPath = BranchPathUtils.createPath(in.readUTF());
			importConfiguration.setBranchPath(branchPath.getPath());
			userId = in.readString();

			// XXX: source kind is always FILES, since the server just receives a bunch of them
			importConfiguration.setSourceKind(ImportSourceKind.FILES);
			importConfiguration.setVersion(in.readEnum(ContentSubType.class));
			importConfiguration.setLanguageRefSetId(in.readString());
			importConfiguration.setCreateVersions(in.readBoolean());
			
			final int exludedRefSetIdCount = in.readInt();
			
			for (int i = 0; i < exludedRefSetIdCount; i++) {
				importConfiguration.excludeRefSet(in.readString());
			}
			
			monitor.worked();
			
			receivedFilesDirectory = Files.createTempDir();
			receivedFilesDirectory.deleteOnExit();
			
			readComponent(in, receivedFilesDirectory, new FileCallback() { @Override public void setFile(final File f) { importConfiguration.setConceptsFile(f); }}, monitor.fork());
			readComponent(in, receivedFilesDirectory, new FileCallback() { @Override public void setFile(final File f) { importConfiguration.setDescriptionsFile(f); }}, monitor.fork());
			readComponent(in, receivedFilesDirectory, new FileCallback() { @Override public void setFile(final File f) { importConfiguration.setTextDefinitionFile(f); }}, monitor.fork());
			readComponent(in, receivedFilesDirectory, new FileCallback() { @Override public void setFile(final File f) { importConfiguration.setRelationshipsFile(f); }}, monitor.fork());
			readComponent(in, receivedFilesDirectory, new FileCallback() { @Override public void setFile(final File f) { importConfiguration.setStatedRelationshipsFile(f); }}, monitor.fork());
			readComponent(in, receivedFilesDirectory, new FileCallback() { @Override public void setFile(final File f) { importConfiguration.setDescriptionType(f); }}, monitor.fork());
			readComponent(in, receivedFilesDirectory, new FileCallback() { @Override public void setFile(final File f) { importConfiguration.setLanguageRefSetFile(f); }}, monitor.fork());
			
			final int refSetUrlCount = in.readInt();
			
			refSetSubmonitor = monitor.fork();
			refSetSubmonitor.begin(refSetUrlCount);
			
			for (int i = 0; i < refSetUrlCount; i++) {
				// XXX: assume that for the pre-determined number of additional refsets, a boolean value of "true" will always be sent 
				readComponent(in, receivedFilesDirectory, new FileCallback() { @Override public void setFile(final File f) { addRefSetUrl(f); }}, refSetSubmonitor.fork());
			}
			
		} finally {
			
			if (null != refSetSubmonitor) {
				refSetSubmonitor.done();
			}
			
			monitor.done();
		}
	}

	private void addRefSetUrl(final File f) {
		
		try {
			importConfiguration.addRefSetSource(f.toURI().toURL());
		} catch (final MalformedURLException e) {
			throw new IORuntimeException(e);
		}
	}

	private void readComponent(final ExtendedDataInputStream in, final File receivedFilesDirectory, final FileCallback fileCallback, final OMMonitor monitor) throws IOException {
		if (in.readBoolean()) {
			readFileContents(in, receivedFilesDirectory, fileCallback, monitor);
		}
	}

	private void readFileContents(final ExtendedDataInputStream in, final File receivedFilesDirectory, final FileCallback fileCallback, 
			final OMMonitor monitor) throws IOException {

		monitor.begin();
		Async async = null;
		FileOutputStream receivedFileOutputStream = null;
		
		try {
			async = monitor.forkAsync();
			
			final String mappedName = in.readString();
			final int fileSize = in.readInt();
			final File receivedFile = File.createTempFile("received", ".txt", receivedFilesDirectory);
			receivedFile.deleteOnExit();
			
			receivedFileOutputStream = new FileOutputStream(receivedFile);
			IOUtil.copy(in, receivedFileOutputStream, fileSize, new byte[SnomedImportProtocolConstants.BUFFER_SIZE]);
			
			fileCallback.setFile(receivedFile);
			importConfiguration.addReleaseFileNameMapping(importConfiguration.toURL(receivedFile).getPath(), mappedName);
			
		} finally {
			
			Closeables.closeQuietly(receivedFileOutputStream);
			
			if (null != async) {
				async.stop();
			}
			
			monitor.done();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.net4j.signal.IndicationWithMonitoring#responding(org.eclipse.net4j.util.io.ExtendedDataOutputStream, org.eclipse.net4j.util.om.monitor.OMMonitor)
	 */
	@Override protected void responding(final ExtendedDataOutputStream out, final OMMonitor monitor) throws Exception {

		monitor.begin(1.3);
		Async async = null;
		
		try {
			
			async = monitor.forkAsync();
			
			final SnomedImportResult importResult = new ImportUtil().doImport(userId, importConfiguration, new ConsoleProgressMonitor());
			
			out.writeInt(importResult.getVisitedConcepts().size());
			out.writeInt(importResult.getVisitedRefSets().size());
			out.writeInt(importResult.getValidationDefects().size());
			
			for (final SnomedConceptIndexEntry visitedConcept : importResult.getVisitedConcepts()) {
				out.writeObject(visitedConcept);
			}
			
			for (final SnomedRefSetIndexEntry visitedRefSet : importResult.getVisitedRefSets()) {
				out.writeObject(visitedRefSet);
			}
			
			for (final SnomedValidationDefect validationDefect : importResult.getValidationDefects()) {
				out.writeObject(validationDefect);
			}
			
		} finally {
			
			// Also removes directory contents
//			IOUtil.delete(receivedFilesDirectory);
			
			if (null != async) {
				async.stop();
			}
			
			monitor.done();
		}
	}
}