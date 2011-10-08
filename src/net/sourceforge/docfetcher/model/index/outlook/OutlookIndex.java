/*******************************************************************************
 * Copyright (c) 2010, 2011 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.model.index.outlook;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import net.sourceforge.docfetcher.model.Cancelable;
import net.sourceforge.docfetcher.model.DocumentType;
import net.sourceforge.docfetcher.model.Path;
import net.sourceforge.docfetcher.model.TreeIndex;
import net.sourceforge.docfetcher.model.UtilModel;
import net.sourceforge.docfetcher.model.index.IndexWriterAdapter;
import net.sourceforge.docfetcher.model.index.IndexingError;
import net.sourceforge.docfetcher.model.index.IndexingError.ErrorType;
import net.sourceforge.docfetcher.model.index.IndexingException;
import net.sourceforge.docfetcher.model.index.IndexingReporter;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.RecursiveMethod;

import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.pff.PSTException;
import com.pff.PSTFile;
import com.pff.PSTFolder;
import com.pff.PSTMessage;

/**
 * @author Tran Nam Quang
 */
@SuppressWarnings("serial")
public final class OutlookIndex extends TreeIndex<MailDocument, MailFolder> {
	
	/*
	 * Tests indicate we can directly retrieve any message via its ID, but not
	 * attachments. This means in order to retrieve an attachment, we'll have to
	 * fetch its message first.
	 * 
	 * Tests also indicate that nothing serious happens if we read a PST file
	 * that is currently open in Outlook. Since we're only reading the PST file,
	 * the worst that could happen is to get some garbage out of the PST file,
	 * as a result of Outlook writing to it concurrently.
	 */
	
	private MailFolder simplifiedRootFolder;
	
	public OutlookIndex(@Nullable File indexParentDir, @NotNull File pstFile) {
		super(indexParentDir, pstFile);
	}
	
	@NotNull
	protected String getIndexDirName(@NotNull File pstFile) {
		return Util.splitFilename(pstFile)[0];
	}
	
	@NotNull
	protected MailFolder createRootFolder(@NotNull Path path) {
		return new MailFolder(path);
	}
	
	public boolean isEmailIndex() {
		return true;
	}
	
	public DocumentType getDocumentType() {
		return DocumentType.OUTLOOK;
	}
	
	public IndexingResult update(	@NotNull IndexingReporter reporter,
									@NotNull Cancelable cancelable) {
		if (cancelable.isCanceled())
			return IndexingResult.SUCCESS_UNCHANGED;
		
		reporter.setStartTime(System.currentTimeMillis());
		MailFolder rootFolder = getRootFolder();
		rootFolder.setError(null);
		IndexWriterAdapter writer = null;
		
		try {
			/*
			 * Return immediately if the last-modified field of the PST file
			 * hasn't changed.
			 */
			File rootFile = getCanonicalRootFile();
			long newLastModified = rootFile.lastModified();
			if (UtilModel.isUnmodifiedArchive(rootFolder, newLastModified))
				return IndexingResult.SUCCESS_UNCHANGED;
			rootFolder.setLastModified(newLastModified);
			
			writer = new IndexWriterAdapter(getLuceneDir());
			OutlookContext context = new OutlookContext(
					getConfig(), writer, reporter, cancelable
			);
			PSTFile pstFile = new PSTFile(rootFile.getPath());
			visitFolder(context, rootFolder, pstFile.getRootFolder());
			
			simplifiedRootFolder = new TreeRootSimplifier<MailFolder> () {
				protected boolean hasContent(MailFolder node) {
					return node.getDocumentCount() > 0;
				}
				protected boolean hasDeepContent(MailFolder node) {
					return node.hasDeepContent();
				}
				protected Collection<MailFolder> getChildren(MailFolder node) {
					return node.getSubFolders();
				}
			}.getSimplifiedRoot(getRootFolder());
			
			writer.optimize();
			return IndexingResult.SUCCESS_CHANGED;
		}
		catch (PSTException e) {
			report(reporter, e);
		}
		catch (IOException e) {
			report(reporter, e);
		}
		catch (IndexingException e) {
			report(reporter, e.getIOException());
		}
		finally {
			Closeables.closeQuietly(writer);
			reporter.setEndTime(System.currentTimeMillis());
		}
		return IndexingResult.FAILURE;
	}
	
	private void report(@NotNull IndexingReporter reporter,
						@NotNull Exception e) {
		MailFolder rootFolder = getRootFolder();
		IndexingError error = new IndexingError(
			ErrorType.IO_EXCEPTION, rootFolder, e);
		rootFolder.setError(error);
		reporter.fail(error);
	}
	
	// TODO now: method currently not in use
	@NotNull
	public MailFolder getSimplifiedRootFolder() {
		if (simplifiedRootFolder == null) // update method hasn't been called yet
			return getRootFolder();
		return simplifiedRootFolder;
	}
	
	// TODO doc: stores in the given folder whether it has 'deep' content or not.
	@RecursiveMethod
	private static void visitFolder(@NotNull OutlookContext context,
									@NotNull MailFolder folder,
									@NotNull PSTFolder pstFolder)
			throws IndexingException, PSTException {
		folder.setHasDeepContent(false);
		final Map<String, MailDocument> unseenMails = Maps.newHashMap(folder.getDocumentMap());
		final Map<String, MailFolder> unseenSubFolders = Maps.newHashMap(folder.getSubFolderMap());
		
		// Visit mails
		if (pstFolder.getContentCount() > 0) {
			try {
				PSTMessage pstMail = (PSTMessage) pstFolder.getNextChild();
				while (pstMail != null) {
					if (context.isStopped()) break;
					/*
					 * Note: The user should not be allowed to stop the indexing
					 * in the middle of email processing (e.g. between
					 * attachments), otherwise we could end up indexing emails
					 * which have only one half of all attachments, which would
					 * complicate email modification detection.
					 * 
					 * Note: For the email UID, we'll use the 'descriptor node
					 * ID' rather than the 'internet message ID', for several
					 * reasons: (1) Not every email has an internet message ID,
					 * e.g. unsent emails. (2) The descriptor node ID is only an
					 * internal ID used by Outlook, but it does not change. (3)
					 * The descriptor node ID allows fast retrieval of single
					 * emails, which is what we need for the preview.
					 */
					String id = String.valueOf(pstMail.getDescriptorNodeId());
					long newLastModified = pstMail.getLastModificationTime().getTime();
					MailDocument mail = unseenMails.remove(id);
					if (mail == null) { // Mail added
						String subject = pstMail.getSubject();
						mail = new MailDocument(
							folder, id, subject, newLastModified);
						context.index(mail, pstMail, true);
					} else if (mail.isModified(newLastModified)) { // Mail modified
						/*
						 * Note: Outlook mails are not immutable, because
						 * Outlook allows modifying the subject and body, as
						 * well as removing attachments. Such modifications will
						 * alter the last-modified value as provided by the
						 * PSTMessage object. It is not clear though whether any
						 * other changes are possible, and if so, whether we can
						 * rely on the last-modified value to reflect such
						 * changes.
						 */
						mail.setLastModified(newLastModified);
						context.index(mail, pstMail, false);
					}
					pstMail = (PSTMessage) pstFolder.getNextChild();
				}
			} catch (IOException e) {
				throw new IndexingException(e);
			}
			folder.setHasDeepContent(true);
		}
		
		// Visit subfolders
		if (pstFolder.hasSubfolders()) {
			try {
				for (PSTFolder pstSubFolder : pstFolder.getSubFolders()) {
					if (context.isStopped()) break;
					String foldername = pstSubFolder.getDisplayName();
					MailFolder subFolder = unseenSubFolders.remove(foldername);
					if (subFolder == null) {
						Path path = folder.getPath().createSubPath(foldername);
						subFolder = new MailFolder(path);
						folder.putSubFolder(subFolder);
					}
					visitFolder(context, subFolder, pstSubFolder);
					if (subFolder.hasDeepContent())
						folder.setHasDeepContent(true);
				}
			} catch (IOException e) {
				throw new IndexingException(e);
			}
		}

		if (context.isStopped()) return;
		
		// Handle missing mails and folders
		for (MailDocument mail : unseenMails.values()) {
			folder.removeDocument(mail);
			context.deleteFromIndex(mail.getUniqueId());
		}
		for (MailFolder subFolder : unseenSubFolders.values())
			folder.removeSubFolder(subFolder);
	}

}
